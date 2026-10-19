package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTemplateConfirmConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitTemplateConfirmConfig.class);

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
}
