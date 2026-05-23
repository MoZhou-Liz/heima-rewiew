package com.hmdp.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hmdp.entity.SeckillVoucher;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    //caffeine初始化
    @Bean
    public Cache<Long, SeckillVoucher> shopCache() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10000)
                .expireAfterWrite(5, TimeUnit.MINUTES) //5分钟后本地缓存自动失效,读redis
                .build();
    }
    //redisson布隆过滤器初始化
    @Bean
    public RBloomFilter<Long> voucherBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("seckill:bloom:filter");
        bloomFilter.tryInit(1000000L,0.01); //优惠券最大数100万,允许误判1%
        return bloomFilter;
    }
}
