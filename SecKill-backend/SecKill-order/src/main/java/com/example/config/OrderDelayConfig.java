package com.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 订单支付超时自动关闭：TTL 延迟队列 + 死信转发
 *
 * <p>下单成功后发消息到 {@link #ORDER_DELAY_QUEUE}，消息在队列里等到 TTL 到期，
 * 由 RabbitMQ 自动转入 {@link #ORDER_CLOSE_QUEUE}，消费者收到即代表"该关闭这张单了"，
 * 再查库确认是否仍为待支付，避免用定时任务全表扫描。</p>
 *
 * <p>注意：队列级 TTL 对队列中所有消息生效，且只在消息到达队头时判断过期。
 * 因为所有订单的支付超时时间一致，不存在队头阻塞问题。</p>
 */
@Configuration
public class OrderDelayConfig {

    /**延迟交换机：下单后消息先进这里*/
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    /**延迟队列：消息在此停留 TTL 时间，到期被转入关闭队列*/
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay.close";

    /**关闭交换机 / 队列：消费者监听这里执行真正的关单*/
    public static final String ORDER_CLOSE_EXCHANGE = "order.close.exchange";
    public static final String ORDER_CLOSE_QUEUE = "order.close.queue";
    public static final String ORDER_CLOSE_ROUTING_KEY = "order.close.handle";

    private final Duration payTimeout;

    public OrderDelayConfig(@Value("${order.pay-timeout-minutes:15}") long payTimeoutMinutes) {
        this.payTimeout = Duration.ofMinutes(payTimeoutMinutes);
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                // TTL 到期后按下面的路由规则转入关闭队列
                .withArgument("x-dead-letter-exchange", ORDER_CLOSE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CLOSE_ROUTING_KEY)
                .withArgument("x-message-ttl", payTimeout.toMillis())
                .build();
    }

    @Bean
    public DirectExchange orderDelayExchange() {
        return ExchangeBuilder.directExchange(ORDER_DELAY_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderCloseExchange() {
        return ExchangeBuilder.directExchange(ORDER_CLOSE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderCloseQueue() {
        return QueueBuilder.durable(ORDER_CLOSE_QUEUE).build();
    }

    @Bean
    public Binding orderCloseBinding() {
        return BindingBuilder.bind(orderCloseQueue())
                .to(orderCloseExchange())
                .with(ORDER_CLOSE_ROUTING_KEY);
    }
}
