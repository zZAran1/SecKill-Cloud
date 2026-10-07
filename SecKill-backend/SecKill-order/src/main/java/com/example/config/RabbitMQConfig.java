package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order.create";

    // 死信交换机
    public static final String SECKILL_ORDER_DLX_EXCHANGE = "seckill.order.dlx.exchange";
    // 死信队列
    public static final String SECKILL_ORDER_DLX_QUEUE = "seckill.order.dlx.queue";
    // 死信路由键
    public static final String SECKILL_ORDER_DLX_ROUTING_KEY = "seckill.order.dlx.routing-key";

    // 在类中添加以下字段和方法
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Autowired
    public void initRabbitTemplate(RabbitTemplate rabbitTemplate) {
        // Confirm 回调：消息是否到达 Exchange
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息到达 Exchange: {}", correlationData != null ? correlationData.getId() : "null");
            } else {
                log.error("消息未到达 Exchange: {}, cause: {}",
                        correlationData != null ? correlationData.getId() : "null", cause);
                // TODO: 生产环境应记录失败消息，定时任务补偿或人工介入
            }
        });

        // Return 回调：消息是否到达 Queue
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息未到达 Queue: exchange={}, routingKey={}, message={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(),
                    new String(returned.getMessage().getBody()), returned.getReplyText());
            // TODO: 生产环境应记录失败消息，定时任务补偿或人工介入
        });
    }
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SECKILL_ORDER_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange seckillOrderExchange() {
        return ExchangeBuilder.directExchange(SECKILL_ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillOrderDlxExchange() {
        return ExchangeBuilder.directExchange(SECKILL_ORDER_DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue seckillOrderDlxQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_DLX_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderDlxBinding() {
        return BindingBuilder.bind(seckillOrderDlxQueue())
                .to(seckillOrderDlxExchange())
                .with(SECKILL_ORDER_DLX_ROUTING_KEY);
    }
}