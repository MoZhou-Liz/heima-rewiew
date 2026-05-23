package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {
    @Resource
    private Cache<Long, SeckillVoucher> seckillVoucherCache;

    @Resource
    private RBloomFilter<Long> voucherBloomFilter;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public SeckillVoucher queryById(Long id) {
        //布隆过滤器拦截,判断id是否存在
        if (!voucherBloomFilter.contains(id)) {
            return null;
        }
        //caffeine本地拦截,有就直接返回
        SeckillVoucher voucher = seckillVoucherCache.getIfPresent(id);
        if (voucher != null) {
            return voucher;
        }
        //本地缓存没有去redis拿数据
        String key = "cache:seckill:" + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //如果redis有合法的json字符串
        if (StrUtil.isNotBlank(json)) {
            voucher = JSONUtil.toBean(json, SeckillVoucher.class);
            seckillVoucherCache.put(id, voucher);
            return voucher;
        }
        //防穿透.如果命中的不是空白,而是我们故意塞进去的空字符串""
        if (json != null) {
            return null;//返回错误不去查库
        }
        //mysql
        voucher = getById(id);
        //数据库查出啦null()布隆过滤器的漏判
        if (voucher == null) {
            //空标记到redis防止短时间轰炸
            stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
            return null;
        }
        //数据库查到数据,同步写回redis和caffeine
        int randomMinutes = cn.hutool.core.util.RandomUtil.randomInt(1, 6);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(voucher), 30+randomMinutes, TimeUnit.MINUTES);
        seckillVoucherCache.put(id, voucher);
        return voucher;
    }
}
