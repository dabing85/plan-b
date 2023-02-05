package com.dabing.planabc.service.impl;

import cn.hutool.core.util.BooleanUtil;
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

        //2. 判断是否存在 "", null ,"/t/n"都是false "abc"才是true
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在 返回数据
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //判断是否是空值 此时shopJson可能是 "", null ,"/t/n"三种情况
        if(shopJson != null ){
            //此时shopJson=="" 是我们缓存的空对象
            return Result.fail("店铺信息不存在！");
        }
        //4.此时shopJson为null 查询数据库
        Shop shop=getById(id);
        if(shop == null){
            //5.数据库不存在 返回错误
            //缓存空对象 2分钟超时
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        //6.数据库存在 写入redis，添加60分钟的超时时间 返回数据
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    /**
     * 解决缓存击穿问题 - 基于互斥锁
     */
    @Override
    public Result queryShopHCJCByLock(Long id) {
        String key=CACHE_SHOP_KEY+id;
        //1. 从redis查询商铺缓存
        //前面用户信息用了hash类型，这里试试string类型
        String shopJson=stringRedisTemplate.opsForValue().get(key);

        //2. 判断是否存在 "", null ,"/t/n"都是false "abc"才是true
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在 返回数据
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //判断是否是空值 此时shopJson可能是 "", null ,"/t/n"三种情况
        if(shopJson != null ){
            //此时shopJson=="" 是我们缓存的空对象
            return Result.fail("店铺信息不存在！");
        }
        //4.此时shopJson为null 查询数据库
        //解决缓存击穿问题 - 基于互斥锁
        //4.1 获取互斥锁 - redis的String类型的setnx命令可以实现，因为它只有当这个值不存在的时候才能添加
        //如：setnx lock 1 -- 表示获取了锁      其他线程要setnx的时候不行
        //   del lock     --删除这个key，即释放锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean flag = tryLock(lockKey);
        Shop shop= null;
        try {
            //4.2判断是否获取锁
            if(!flag){
                //4.3没有获取锁 - 休眠一段时间后重新查询商店缓存
                Thread.sleep(50);
                return queryShopHCJCByLock(id); //递归再查询查询店铺缓存
            }
            //4.4获取到锁 - 进行数据查询 并写入缓存，返回数据，释放锁
            shop = getById(id);
            Thread.sleep(100);
            if(shop == null){
                //5.数据库不存在 返回错误
                //缓存空对象 2分钟超时
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return Result.fail("店铺不存在");
            }
            //6.数据库存在 写入redis，添加60分钟的超时时间 返回数据
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放锁
            unLock(lockKey);
        }
        return Result.ok(shop);
    }

    /**
     * 获取互斥锁
     */
    private boolean tryLock(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    /**
     * 释放锁
     */
    private void unLock(String key){
        stringRedisTemplate.delete(key);
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




