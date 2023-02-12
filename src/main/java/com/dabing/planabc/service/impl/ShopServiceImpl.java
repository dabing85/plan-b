package com.dabing.planabc.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Shop;
import com.dabing.planabc.mapper.ShopMapper;
import com.dabing.planabc.service.ShopService;
import com.dabing.planabc.utils.RedisClient;
import com.dabing.planabc.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Resource
    private RedisClient redisClient;

    /**
     * 创建线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryShopByType(Integer typeId, Integer current) {
        return null;
    }

    @Override
    public Result queryShopById(Long id) {

        //缓存穿透
        Shop shop = redisClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //逻辑过期解决缓存击穿
        //Shop shop = redisClient.queryWithLogicExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, LOCK_SHOP_TTL, TimeUnit.SECONDS);
        //互斥锁解决缓存击穿
        //Shop shop = redisClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
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
     * 解决缓存击穿问题 - 基于逻辑过期
     * 热点key问题，要事先将key存储到缓存，没有过期时间，因此永远都会命中，如果不命中，则证明不是热点key。
     * 但是有逻辑过期时间，这个逻辑过期时间需要额外借助一个自定义类RedisData，该类属性是对应的店铺对象和逻辑过期时间
     */
    @Override
    public Result queryShopHCJCByLogicalExpire(Long id) {
        String key=CACHE_SHOP_KEY+id;
        //1. 从redis查询商铺缓存
        String DataJson=stringRedisTemplate.opsForValue().get(key);

        //2. 判断是否命中
        if(!StrUtil.isNotBlank(DataJson)){
            //3.未命中 返回空
            return Result.fail("非热点key");
        }
        //4.命中 判断是否过期
        //解决缓存击穿问题 - 基于逻辑过期
        RedisData redisData = JSONUtil.toBean(DataJson, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //TODO 验证这样直接对象强转行不行
        //JSONObject不能直接强转成Shop会报错
        JSONObject jsonObject = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(jsonObject, Shop.class);
        if(LocalDateTime.now().isBefore(expireTime)){
            //未过期 直接返回数据
            return Result.ok(shop);
        }

        //5 已过期 获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean flag = tryLock(lockKey);
        //不管是否获取到锁，都会返回旧数据
        //获取到锁，开启独立线程，进行缓存重建
        if(flag){
            //二次判断是否过期
            String DataJson2=stringRedisTemplate.opsForValue().get(key);
            RedisData redisData2 = JSONUtil.toBean(DataJson2, RedisData.class);
            LocalDateTime expireTime2 = redisData2.getExpireTime();
            if(LocalDateTime.now().isBefore(expireTime2)){
                //未过期 直接返回数据
                return Result.ok(shop);
            }
            //开启线程
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //重建缓存
                    saveShop2Cache(id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
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

    /**
     * 将热点店铺保存到缓存
     */
    public void saveShop2Cache(Long id){
        String key=CACHE_SHOP_KEY+id;
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(10)); //存入10秒后逻辑过期
        String jsonStr = JSONUtil.toJsonStr(redisData);
        stringRedisTemplate.opsForValue().set(key,jsonStr);
    }
}




