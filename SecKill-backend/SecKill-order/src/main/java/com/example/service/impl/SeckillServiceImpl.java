package com.example.service.impl;

import com.example.config.RabbitMQConfig;
import com.example.exception.BaseBusinessException;
import com.example.message.SeckillOrderMessage;
import com.example.model.dto.SeckillActivityDTO;
import com.example.result.ErrorCode;
import com.example.security.UserContext;
import com.example.service.SeckillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {

    private static final String ACTIVITY_KEY = "seckill:activity:";
    private static final String STOCK_KEY = "seckill:stock:";
    private static final String USERS_KEY = "seckill:users:";

    // Lua 脚本：原子完成 防重检查 + 库存检查 + 扣库存 + 记录用户
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();
    static {
        SECKILL_SCRIPT.setScriptText("""
            if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                return -5
            end
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil or stock <= 0 then
                return -4
            end
            redis.call('DECR', KEYS[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1
            """);
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    public SeckillServiceImpl(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void seckill(Long activityId) {
        Long userId = UserContext.getUserId();

        // 1. 读活动信息（Redis）
        String activityJson = redisTemplate.opsForValue().get(ACTIVITY_KEY + activityId);
        if (activityJson == null) {
            throw new BaseBusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
        SeckillActivityDTO activity;
        try {
            activity = objectMapper.readValue(activityJson, SeckillActivityDTO.class);
        } catch (Exception e) {
            throw new BaseBusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }

        // 2. 时间窗口校验
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BaseBusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BaseBusinessException(ErrorCode.SECKILL_ENDED);
        }

        // 3. Lua 原子扣减
        Long result = redisTemplate.execute(SECKILL_SCRIPT,
                List.of(STOCK_KEY + activityId, USERS_KEY + activityId),
                String.valueOf(userId));
        if (result == null) {
            throw new BaseBusinessException(ErrorCode.SYSTEM_ERROR);
        }
        if (result == -5) {
            throw new BaseBusinessException(ErrorCode.SECKILL_REPEAT);
        }
        if (result == -4) {
            throw new BaseBusinessException(ErrorCode.SECKILL_SOLD_OUT);
        }

        // 4. 发送MQ异步建单（带 CorrelationData，用于 Confirm 回调追踪）
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setUserId(userId);
        message.setProductId(activity.getProductId());
        message.setActivityId(activityId);
        message.setSeckillPrice(activity.getSeckillPrice());

        // 使用订单号作为 CorrelationId，便于追踪
        org.springframework.amqp.rabbit.connection.CorrelationData correlationData =
                new org.springframework.amqp.rabbit.connection.CorrelationData("seckill-" + userId + "-" + activityId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                message,
                correlationData  // 携带 CorrelationData
        );
    }
}