package com.hmdp.config;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import org.redisson.api.RBloomFilter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.List;
//初始化进行数据预热给布隆过滤器
@Component
public class BloomFilterPreloadProcessor implements CommandLineRunner {

    @Resource
    private RBloomFilter<Long> voucherBloomFilter;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Override
    public void run(String... args) throws Exception {
        //去数据库把所有的秒杀全查出来
        List<SeckillVoucher> list = seckillVoucherService.list();
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SeckillVoucher voucher : list) {
            voucherBloomFilter.add(voucher.getVoucherId());
        }
    }
}
