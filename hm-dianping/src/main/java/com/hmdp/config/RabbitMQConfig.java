package com.hmdp.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class RabbitMQConfig {
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }
    @Bean
    public Queue orderQueue() {
        return new Queue("order.queue",true);
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
}
