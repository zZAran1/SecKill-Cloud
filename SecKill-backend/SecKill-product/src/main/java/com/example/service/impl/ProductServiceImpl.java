package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.converter.ProductConverter;
import com.example.exception.ProductException;
import com.example.mapper.ProductMapper;
import com.example.model.dto.ProductStatusDTO;
import com.example.model.dto.ProductsDTO;
import com.example.model.dto.UpdateProductDTO;
import com.example.model.entity.Products;
import com.example.model.vo.ProductsVO;
import com.example.result.ErrorCode;
import com.example.result.PageResult;
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
    private static final int STATUS_OFF_SHELF = 0;
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
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, UpdateProductDTO dto) {
        Products product = this.getById(id);
        if (product == null) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        this.update(new LambdaUpdateWrapper<Products>()
                .eq(Products::getId, id)
                .set(StringUtils.hasText(dto.getName()), Products::getName, dto.getName())
                .set(dto.getDescription() != null, Products::getDescription, dto.getDescription())
                .set(dto.getPrice() != null, Products::getPrice, dto.getPrice())
                .set(dto.getImageUrl() != null, Products::getImageUrl, dto.getImageUrl())
                .set(dto.getStock() != null, Products::getStock, dto.getStock()));
        // 数据变了，缓存必须失效，否则 10 分钟内读到旧数据
        evictCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, ProductStatusDTO dto) {
        Integer status = dto.getStatus();
        if (status == null || (status != STATUS_ON_SHELF && status != STATUS_OFF_SHELF)) {
            throw new ProductException(ErrorCode.BAD_REQUEST, "状态值不合法，只支持 0 下架 / 1 上架");
        }
        boolean updated = this.update(new LambdaUpdateWrapper<Products>()
                .eq(Products::getId, id)
                .set(Products::getStatus, dto.getStatus()));
        if (!updated) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        evictCache(id);
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

    private void evictCache(Long id) {
        redisTemplate.delete(PRODUCT_CACHE_KEY + id);
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
    public PageResult<ProductsVO> pageProducts(String keyword, Integer status, long page, long size) {
        // status 不传时只返回上架商品（公开接口的安全默认值），管理端显式传 0/1 查看下架或上架
        int queryStatus = (status == null) ? STATUS_ON_SHELF : status;
        Page<Products> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Products>()
                        .like(StringUtils.hasText(keyword), Products::getName, keyword)
                        .eq(Products::getStatus, queryStatus)
                        .orderByDesc(Products::getCreateTime));
        return PageResult.of(
                result.getRecords().stream().map(ProductConverter.INSTANCE::toVO).toList(),
                result.getTotal(), page, size);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(Long id, Integer quantity) {
        boolean updated = this.update(new LambdaUpdateWrapper<Products>()
                .eq(Products::getId, id)
                .setSql("stock = stock + " + quantity));
        if (!updated) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
