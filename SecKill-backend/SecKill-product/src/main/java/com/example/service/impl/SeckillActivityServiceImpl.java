package com.example.service.impl;

import com.example.exception.ProductException;
import com.example.mapper.SeckillActivityMapper;
import com.example.model.entity.SeckillActivity;
import com.example.result.ErrorCode;
import com.example.service.SeckillActivityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SeckillActivityServiceImpl implements SeckillActivityService {

    private static final String ACTIVITY_KEY = "seckill:activity:";
    private static final String STOCK_KEY = "seckill:stock:";

    private final SeckillActivityMapper seckillActivityMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SeckillActivityServiceImpl(SeckillActivityMapper seckillActivityMapper,
                                      StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper) {
        this.seckillActivityMapper = seckillActivityMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void preloadActivity(Long id) {
        SeckillActivity activity = seckillActivityMapper.selectById(id);
        if (activity == null) {
            throw new ProductException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
        if (activity.getStatus() != 1) {
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
}