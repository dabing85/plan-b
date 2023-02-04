package com.dabing.planabc.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Shop;
import com.dabing.planabc.mapper.ShopMapper;
import com.dabing.planabc.service.ShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.dabing.planabc.utils.RedisConstants.*;

/**
* @author 22616
* @description 针对表【tb_shop】的数据库操作Service实现
* @createDate 2022-12-16 13:19:34
*/
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
    implements ShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopByType(Integer typeId, Integer current) {
        return null;
    }

    @Override
    public Result queryShopById(Long id) {
        String key=CACHE_SHOP_KEY+id;
        //1. 从redis查询商铺缓存
        //前面用户信息用了hash类型，这里试试string类型
        String shopJson=stringRedisTemplate.opsForValue().get(key);

        //2. 判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在 返回数据
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //4.不存在 查询数据库
        Shop shop=getById(id);
        if(shop == null){
            //5.数据库不存在 返回错误
            return Result.fail("店铺不存在");
        }
        //6.数据库存在 写入redis，添加60分钟的超时时间 返回数据
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    /**
     * 缓存更新策略：先更新数据库，再删除缓存
     */
    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        if(shop == null){
            return Result.fail("店铺信息不能为空！");
        }
        //1.更新数据库
        updateById(shop);

        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
        return Result.ok();
    }
}




