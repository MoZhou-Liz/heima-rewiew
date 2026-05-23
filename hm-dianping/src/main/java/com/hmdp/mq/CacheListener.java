package com.hmdp.mq;

import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.entity.SeckillVoucher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class CacheListener {
    @Resource
    private Cache<Long, SeckillVoucher> seckillVoucherCache;

    @RabbitListener(queues = "#{cacheSyncQueue.name}")
    public void handleCacheSync(Long voucherId) {
        if (voucherId != null) {
            log.info("收到分布式缓存同步广播。正在清除本地内存中券 ID 为 {} 的脏缓存...", voucherId);
            // 收到广播，删除当前服务器 JVM 本地内存里的这个券缓存
            seckillVoucherCache.invalidate(voucherId);
        }
    }
}
