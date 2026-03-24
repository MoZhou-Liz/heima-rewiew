package com.hmdp.service.impl;

import cn.hutool.core.lang.hash.Hash;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result shopTypeService() {
        Map<Object,Object> shopTypeHash = stringRedisTemplate.opsForHash().entries(CACHE_SHOP_TYPE_KEY);
        if(!shopTypeHash.isEmpty()){
            List<ShopType> typeList=shopTypeHash.values().stream()
                    .map(json -> JSONUtil.toBean((String) json,ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(typeList);
        }
        List<ShopType> typeList = this.list();
        if(typeList.isEmpty()){
            return Result.fail("店铺类型为空");
        }
        for(ShopType type:typeList){
            stringRedisTemplate.opsForHash().put(CACHE_SHOP_TYPE_KEY,type.getId().toString(),JSONUtil.toJsonStr(type));
        }

        return Result.ok(typeList);
    }
}
