package com.example.service.impl;

import com.example.config.RabbitMQConfig;
import com.example.constant.SeckillRedisKeys;
import com.example.exception.BaseBusinessException;
import com.example.message.SeckillOrderMessage;
import com.example.model.OrderStatus;
import com.example.model.dto.SeckillActivityDTO;
import com.example.model.entity.Order;
import com.example.model.vo.SeckillResultVO;
import com.example.result.ErrorCode;
import com.example.security.UserContext;
import com.example.service.OrderService;
import com.example.service.SeckillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {
    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final OrderService orderService;

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

    public SeckillServiceImpl(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              RabbitTemplate rabbitTemplate,
                              OrderService orderService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.orderService = orderService;
    }

    @Override
    public void seckill(Long activityId) {
        Long userId = UserContext.getUserId();

        // 1. 读活动信息（Redis）
        String activityJson = redisTemplate.opsForValue().get(SeckillRedisKeys.ACTIVITY_KEY + activityId);
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
                List.of(SeckillRedisKeys.STOCK_KEY + activityId, SeckillRedisKeys.USERS_KEY + activityId),
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

        try{
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                    RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                    message,
                    correlationData  // 携带 CorrelationData
            );
        }catch (Exception e){
            restoreStock(activityId ,userId);
            throw new BaseBusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public SeckillResultVO getResult(Long activityId) {
        Long userId = UserContext.getUserId();

        // 1. 订单已落地（且没被取消）→ 抢购成功
        Order order = orderService.lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getActivityId, activityId)
                .one();
        if (order != null && order.getStatus() != OrderStatus.CANCELLED) {
            return SeckillResultVO.success(order.getOrderNo(), order.getStatus());
        }

        // 2. 没订单但还在已参与集合里 → 消息还在排队建单
        Boolean joined = redisTemplate.opsForSet()
                .isMember(SeckillRedisKeys.USERS_KEY + activityId, String.valueOf(userId));
        if (Boolean.TRUE.equals(joined)) {
            return SeckillResultVO.queuing();
        }

        // 3. 既没订单也没参与标记 → 未抢到（或订单已取消，资格已释放）
        return SeckillResultVO.failed();
    }

    private void restoreStock(Long activityId,Long userId) {
        try{
            redisTemplate.execute(SeckillRedisKeys.RESTORE_SCRIPT,
                    List.of(SeckillRedisKeys.STOCK_KEY + activityId, SeckillRedisKeys.USERS_KEY + activityId),
                    String.valueOf(userId));
        }catch (Exception e){
            log.error("库存回补失败 activityId={}, userId={}",activityId,userId,e);
        }
    }
}
