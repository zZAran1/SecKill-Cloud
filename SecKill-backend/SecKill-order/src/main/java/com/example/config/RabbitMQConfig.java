package com.example.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
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