package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.ProductException;
import com.example.mapper.SeckillActivityMapper;
import com.example.model.dto.SeckillActivityDTO;
import com.example.model.dto.UpdateSeckillActivityDTO;
import com.example.model.entity.Products;
import com.example.model.entity.SeckillActivity;
import com.example.model.vo.SeckillActivityVO;
import com.example.result.ErrorCode;
import com.example.result.PageResult;
import com.example.service.ProductService;
import com.example.service.SeckillActivityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements SeckillActivityService {

    private static final String ACTIVITY_KEY = "seckill:activity:";
    private static final String STOCK_KEY = "seckill:stock:";

    /**0 下线，1 上线*/
    private static final int STATUS_ONLINE = 1;
    private static final int STATUS_OFFLINE = 0;

    private final ProductService productService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SeckillActivityServiceImpl(ProductService productService,
                                      StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper) {
        this.productService = productService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillActivityVO createActivity(SeckillActivityDTO dto) {
        checkTimeWindow(dto.getStartTime(), dto.getEndTime());
        Products product = checkProduct(dto.getProductId());
        if (dto.getSeckillPrice().compareTo(product.getPrice()) > 0) {
            throw new ProductException(ErrorCode.BAD_REQUEST, "秒杀价不能高于商品原价");
        }

        SeckillActivity activity = new SeckillActivity();
        activity.setProductId(dto.getProductId());
        activity.setSeckillPrice(dto.getSeckillPrice());
        activity.setStock(dto.getStock());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setStatus(STATUS_ONLINE);
        this.save(activity);
        return toVO(activity, product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillActivityVO updateActivity(Long id, UpdateSeckillActivityDTO dto) {
        SeckillActivity activity = this.getById(id);
        if (activity == null) {
            throw new ProductException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
        if (!LocalDateTime.now().isBefore(activity.getStartTime())) {
            throw new ProductException(ErrorCode.BAD_REQUEST, "活动已开始，不允许修改");
        }
        checkTimeWindow(dto.getStartTime(), dto.getEndTime());
        Products product = checkProduct(activity.getProductId());
        if (dto.getSeckillPrice().compareTo(product.getPrice()) > 0) {
            throw new ProductException(ErrorCode.BAD_REQUEST, "秒杀价不能高于商品原价");
        }

        activity.setSeckillPrice(dto.getSeckillPrice());
        activity.setStock(dto.getStock());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        this.updateById(activity);

        // 活动参数变了，已预热的 Redis 数据必须作废
        evictCache(id);
        return toVO(activity, product);
    }

    @Override
    public PageResult<SeckillActivityVO> pageActivities(Integer status, long page, long size) {
        // status 不传时只返回上线活动（公开接口的安全默认值），管理端显式传 0/1 查看下线或上线
        int queryStatus = (status == null) ? STATUS_ONLINE : status;
        Page<SeckillActivity> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, queryStatus)
                        .orderByDesc(SeckillActivity::getCreateTime));

        List<SeckillActivity> activities = result.getRecords();
        Map<Long, Products> productMap = batchGetProducts(activities);
        return PageResult.of(
                activities.stream()
                        .map(a -> toVO(a, productMap.get(a.getProductId())))
                        .toList(),
                result.getTotal(), page, size);
    }

    @Override
    public SeckillActivityVO getActivity(Long id) {
        SeckillActivity activity = this.getById(id);
        if (activity == null) {
            throw new ProductException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
        return toVO(activity, productService.getById(activity.getProductId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void online(Long id) {
        boolean updated = this.update(new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, id)
                .eq(SeckillActivity::getStatus, STATUS_OFFLINE)
                .set(SeckillActivity::getStatus, STATUS_ONLINE));
        if (!updated && this.getById(id) == null) {
            throw new ProductException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        boolean updated = this.update(new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, id)
                .eq(SeckillActivity::getStatus, STATUS_ONLINE)
                .set(SeckillActivity::getStatus, STATUS_OFFLINE));
        if (!updated && this.getById(id) == null) {
            throw new ProductException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
        // 下线后立即停止接单，Redis 中的活动信息与库存作废
        evictCache(id);
    }

    @Override
    public void preloadActivity(Long id) {
        SeckillActivity activity = this.getById(id);
        if (activity == null) {
            throw new ProductException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
        if (activity.getStatus() != STATUS_ONLINE) {
            throw new ProductException(ErrorCode.SECKILL_ENDED);
        }

        // TTL 对齐活动结束时间 + 1 小时缓冲，结束自动清理不脏数据
        long ttlSeconds = Duration.between(LocalDateTime.now(), activity.getEndTime()).getSeconds() + 3600;
        if (ttlSeconds <= 3600) {
            throw new ProductException(ErrorCode.SECKILL_ENDED);
        }

        // 1. 活动信息 JSON（不含库存）
        Map<String, Object> info = new HashMap<>();
        info.put("id", activity.getId());
        info.put("productId", activity.getProductId());
        info.put("seckillPrice", activity.getSeckillPrice());
        info.put("startTime", activity.getStartTime().toString());
        info.put("endTime", activity.getEndTime().toString());
        try {
            redisTemplate.opsForValue().set(ACTIVITY_KEY + id, objectMapper.writeValueAsString(info),
                    Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new ProductException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }

        // 2. 库存独立 key
        redisTemplate.opsForValue().set(STOCK_KEY + id, String.valueOf(activity.getStock()),
                Duration.ofSeconds(ttlSeconds));
    }

    // ---------- 私有辅助 ----------

    private void checkTimeWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new ProductException(ErrorCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
    }

    private Products checkProduct(Long productId) {
        Products product = productService.getById(productId);
        if (product == null) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private Map<Long, Products> batchGetProducts(List<SeckillActivity> activities) {
        List<Long> productIds = activities.stream()
                .map(SeckillActivity::getProductId)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(Products::getId, Function.identity()));
    }

    private SeckillActivityVO toVO(SeckillActivity activity, Products product) {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setProductId(activity.getProductId());
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setStock(activity.getStock());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setCreateTime(activity.getCreateTime());
        vo.setRemainingStock(readRemainingStock(activity));
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getImageUrl());
            vo.setOriginalPrice(product.getPrice());
        }
        return vo;
    }

    /**Redis 有实时库存就用实时值，未预热时回落到 DB 库存*/
    private Integer readRemainingStock(SeckillActivity activity) {
        String cached = redisTemplate.opsForValue().get(STOCK_KEY + activity.getId());
        if (cached == null) {
            return activity.getStock();
        }
        try {
            return Integer.parseInt(cached);
        } catch (NumberFormatException e) {
            return activity.getStock();
        }
    }

    private void evictCache(Long id) {
        redisTemplate.delete(ACTIVITY_KEY + id);
        redisTemplate.delete(STOCK_KEY + id);
    }
}
