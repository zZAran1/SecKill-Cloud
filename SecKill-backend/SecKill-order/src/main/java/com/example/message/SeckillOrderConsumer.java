package com.example.message;

import com.example.config.RabbitMQConfig;
import com.example.exception.BaseBusinessException;
import com.example.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Component
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);
    public SeckillOrderConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)
    public void onMessage(SeckillOrderMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            orderService.createSeckillOrder(
                    message.getUserId(),
                    message.getProductId(),
                    message.getSeckillPrice(),
                    message.getActivityId());
            channel.basicAck(deliveryTag, false);
        } catch (BaseBusinessException e) {
            // 业务异常：不可重试，直接确认避免死循环，记录日志
            log.error("秒杀订单业务异常: userId={}, productId={}, msg={}",
                    message.getUserId(), message.getProductId(), e.getMessage());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 系统异常：requeue=false，消息进入死信队列（由 SeckillOrderDlqConsumer 兜底处理）
            log.error("秒杀订单系统异常，将进入死信队列: userId={}, productId={}",
                    message.getUserId(), message.getProductId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}