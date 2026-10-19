package com.example.message;

import com.example.config.RabbitMQConfig;
import com.example.constant.SeckillRedisKeys;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class SeckillOrderDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderDlqConsumer.class);

    private final StringRedisTemplate redisTemplate;

    public SeckillOrderDlqConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_DLX_QUEUE)
    public void onDlqMessage(SeckillOrderMessage message, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Long activityId = message.getActivityId();
            Long userId = message.getUserId();

            // 1. 回补 Redis 库存并解除限购标记（订单没建成，但 Lua 已经扣了库存）
            redisTemplate.execute(SeckillRedisKeys.RESTORE_SCRIPT,
                    List.of(SeckillRedisKeys.STOCK_KEY + activityId, SeckillRedisKeys.USERS_KEY + activityId),
                    String.valueOf(userId));

            // 2. 记录告警日志（生产环境应接入告警系统：钉钉/企微/邮件）
            log.error("【秒杀订单死信】订单创建失败，已回补库存。activityId={}, userId={}, productId={}, price={}",
                    activityId, userId, message.getProductId(), message.getSeckillPrice());

            // 3. 确认死信消息，避免再次投递
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            // 死信消费本身失败，记录日志但不 requeue，避免死信队列死循环
            log.error("【死信消费失败】消息处理异常，需人工介入。message={}", message, e);
            channel.basicAck(deliveryTag, false);
        }
    }
}
