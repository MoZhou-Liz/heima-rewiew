package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private Cache<Long,SeckillVoucher> seckillVoucherCache;
    @Resource
    private RBloomFilter<Long> voucherBloomFilter;
    @Resource
    public StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);

        //没有优惠券直接返回空列表，不往下走浪费内存
        if(vouchers==null||vouchers.isEmpty()){
            return Result.ok(java.util.Collections.EMPTY_LIST);
        }
        for(Voucher voucher:vouchers){
            //只有秒杀券才去多级缓存查高频变动的库存和时间
            if(voucher.getType()==1){
                SeckillVoucher seckillVoucher=seckillVoucherService.queryById(voucher.getId());
                //如果在多级缓存里成功查到了对应的秒杀详情，就把最新的同步到前端
                if(seckillVoucher!=null){
                    voucher.setStock(seckillVoucher.getStock());
                    voucher.setBeginTime(seckillVoucher.getBeginTime());
                    voucher.setEndTime(seckillVoucher.getEndTime());
                }
            }
        }
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY+voucher.getId(),voucher.getStock().toString());

        //新券上架,追加到布隆过滤器绿名单
        voucherBloomFilter.add(voucher.getId());
        //删除redis中券可能存在的就缓存(双写一致,先更新数据库再删除缓存)
        stringRedisTemplate.delete("cache:seckill:"+voucher.getId());
        //废弃caffeine的缓存
        seckillVoucherCache.invalidate(voucher.getId());
        rabbitTemplate.convertAndSend("cache.seckill.exchange", "", voucher.getId());
    }
}
