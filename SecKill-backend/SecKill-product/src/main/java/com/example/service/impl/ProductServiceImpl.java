package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.converter.ProductConverter;
import com.example.exception.ProductException;
import com.example.mapper.ProductMapper;
import com.example.model.dto.ProductsDTO;
import com.example.model.entity.Products;
import com.example.model.vo.ProductsVO;
import com.example.result.ErrorCode;
import com.example.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Products> implements ProductService {

    private static final int STATUS_ON_SHELF = 1;
    private static final String PRODUCT_CACHE_KEY = "seckill:product:";
    private static final String CACHE_NULL = "null";
    private static final long CACHE_TTL_SECONDS = 600;
    private static final long NULL_TTL_SECONDS = 60;
    private static final String LOCK_KEY_PREFIX = "seckill:lock:product:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;


    public ProductServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redissonClient = redissonClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(ProductsDTO dto) {
        Products product = ProductConverter.INSTANCE.toEntity(dto);
        product.setStatus(STATUS_ON_SHELF);
        this.save(product);
    }

    @Override
    public ProductsVO getProduct(Long id) {
        String key = PRODUCT_CACHE_KEY + id;

        // 1. 先查缓存
        ProductsVO cached = readCache(key);
        if (cached != null) {
            return cached;
        }

        // 2. 缓存未命中，回源数据库
       RLock lock =redissonClient.getLock(LOCK_KEY_PREFIX + id);
        lock.lock();
        try{
            cached =readCache(key);
            if(cached != null) {
                return cached;
            }
            Products product =this.getById(id);
            if(product == null) {
                redisTemplate.opsForValue().set(key,CACHE_NULL,NULL_TTL_SECONDS,TimeUnit.SECONDS);
                throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            ProductsVO vo = ProductConverter.INSTANCE.toVO(product);
            writeCache(key,vo);
            return vo;
        }finally {
            lock.unlock();
        }
    }

    private ProductsVO readCache(String key){
        String cached =redisTemplate.opsForValue().get(key);
        if(cached==null){
            return null;
        }
        if(CACHE_NULL.equals(cached)){
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        try{
            return objectMapper.readValue(cached,ProductsVO.class);
        }catch (JsonProcessingException e){
            redisTemplate.delete(key);
            return null;
        }
    }

    private void writeCache(String key,ProductsVO vo){
        long ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(300);
        try{
            redisTemplate.opsForValue().set(key,objectMapper.writeValueAsString(vo),ttl,TimeUnit.SECONDS);
        }catch (JsonProcessingException e){}
    }

    @Override
    public List<ProductsVO> listProducts(String keyword) {
        return this.lambdaQuery()
                .like(StringUtils.hasText(keyword), Products::getName, keyword)
                .eq(Products::getStatus, STATUS_ON_SHELF)
                .list()
                .stream()
                .map(ProductConverter.INSTANCE::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long id, Integer quantity) {
        // 乐观锁：WHERE stock >= quantity 保证不会扣成负数
        boolean updated = this.update(new LambdaUpdateWrapper<Products>()
                .eq(Products::getId, id)
                .ge(Products::getStock, quantity)  // stock >= quantity
                .setSql("stock = stock - " + quantity));

        if (!updated) {
            throw new ProductException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT);
        }
    }
}