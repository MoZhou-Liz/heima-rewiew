package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@Configuration
public class RabbitMQConfig {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }

    @Bean
    public Queue orderQueue() {
        return new Queue("order.queue", true);
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with("order.create");
    }

    @Bean
    public FanoutExchange cacheExchange() {
        return new FanoutExchange("cache.seckill.exchange");
    }

    //为当前服务器实例创建一个独一无二，下线自动销毁的专属队列
    @Bean
    public Queue cacheSyncQueue() {
        return new Queue("cache.sync.queue." + UUID.randomUUID().toString(), false, true, true);
    }

    @Bean
    public Binding cacheSyncBinding(Queue cacheSyncQueue, FanoutExchange cacheExchange) {
        return BindingBuilder.bind(cacheSyncQueue).to(cacheExchange);
    }

    @PostConstruct
    public void initRabbitTemplate() {
        // 1. 设置 ConfirmCallback：确认消息是否到达 Broker/Exchange
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("【MQ发送成功】消息成功到达交换机！");
            } else {
                log.error("【MQ发送失败】消息未到达交换机！原因: {}", cause);
            }
        });
        // 2. 设置 ReturnsCallback：确认消息是否成功从 Exchange 路由到 Queue
        // 只有当消息路由失败（比如 RoutingKey 写错）时，这个回调才会被触发
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("MQ路由失败,消息被交换机退回！应答码: {}, 原因: {}, 交换机: {}, 路由键: {}, 消息内容: {}",
                    replyCode, replyText, exchange, routingKey, new String(message.getBody()));
        });
    }
}
