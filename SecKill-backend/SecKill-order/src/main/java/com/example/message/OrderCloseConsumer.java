package com.example.message;

import com.example.config.OrderDelayConfig;
import com.example.service.OrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 监听延迟队列转过来的消息，执行订单超时关闭。
 * 收到消息只代表"超时时间到了"，是否真要关单由 service 查库判断，天然幂等。
 */
@Component
public class OrderCloseConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseConsumer.class);

    private final OrderService orderService;

    public OrderCloseConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = OrderDelayConfig.ORDER_CLOSE_QUEUE)
    public void onMessage(OrderCloseMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            orderService.closeTimeoutOrder(message.getOrderNo());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 关单/回补库存失败已在事务里回滚，这里记录告警待人工补偿。
            // 不 requeue：避免与失败原因无关的消息被无限重投
            log.error("【订单超时关闭失败】orderNo={}，需人工介入", message.getOrderNo(), e);
            channel.basicAck(deliveryTag, false);
        }
    }
}
