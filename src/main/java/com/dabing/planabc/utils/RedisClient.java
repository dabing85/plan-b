package com.dabing.planabc.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dabing.planabc.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.dabing.planabc.utils.RedisConstants.*;

@Slf4j
@Component
public class RedisClient {
    @Resource
    private final StringRedisTemplate stringRedisTemplate;
    private final ExecutorService CACHE_REBUILD_EXECUTOR=Executors.newFixedThreadPool(10);

    public RedisClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 方法一：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     */
    public void set(String key,Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    /**
     * 方法二：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓
     * 存击穿问题
     */
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 方法三：根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     */
    public <R,ID> R queryWithPassThrough(
            String prefKey, ID id, Class<R> type, Function<ID,R> bdCallback,long time,TimeUnit timeUnit){
        String key=prefKey+id;
        //1. 从redis查询商铺缓存
        String jsonStr=stringRedisTemplate.opsForValue().get(key);

        //2. 判断是否存在 "", null ,"/t/n"都是false "abc"才是true
        if(StrUtil.isNotBlank(jsonStr)){
            //3.存在 返回数据
            R r = JSONUtil.toBean(jsonStr, type);
            return r;
        }
        //判断是否是空值 此时shopJson可能是 "", null ,"/t/n"三种情况
        if(jsonStr != null ){
            //此时shopJson=="" 是我们缓存的空对象
            return null;
        }
        //4.此时shopJson为null 查询数据库
        //Shop shop=getById(id);
        //这个方法得让参数传进来
        R r = bdCallback.apply(id);
        if(r == null){
            //5.数据库不存在 返回错误
            //缓存空对象 2分钟超时
            stringRedisTemplate.opsForValue().set(key,"", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6.数据库存在 写入redis，添加60分钟的超时时间 返回数据
        set(key,r,time,timeUnit);
        return r;
    }

    /**
     * 方法四：根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题将逻辑进行封装
     */
    public <R,ID> R queryWithLogicExpire (String perfKey,ID id,Class<R> type,Function<ID,R> dbCallback,long time,TimeUnit timeUnit){
        String key=perfKey+id;
        //1. 从redis查询商铺缓存
        String DataJson=stringRedisTemplate.opsForValue().get(key);

        //2. 判断是否命中
        if(!StrUtil.isNotBlank(DataJson)){
            //3.未命中 返回空
            return null;
        }
        //4.命中 判断是否过期
        //解决缓存击穿问题 - 基于逻辑过期
        RedisData redisData = JSONUtil.toBean(DataJson, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        JSONObject jsonObject = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(jsonObject, type);
        if(LocalDateTime.now().isBefore(expireTime)){
            //未过期 直接返回数据
            return r;
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
                return r;
            }
            //开启线程
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查询数据库
                    R newR = dbCallback.apply(id);
                    //重建缓存
                    setWithLogicalExpire(key,newR,time,timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        return r;
    }

    /**
     * 方法五：根据指定的key查询缓存，并反序列化为指定类型，需要利用互斥锁解决缓存击穿问题将逻辑进行封装
     */
    public <R,ID> R queryWithMutex(String prefKey,ID id,Class<R> type,Function<ID,R> dbCallback,long time,TimeUnit timeUnit){
        String key=prefKey+id;
        //1. 从redis查询商铺缓存
        //前面用户信息用了hash类型，这里试试string类型
        String jsonStr=stringRedisTemplate.opsForValue().get(key);

        //2. 判断是否存在 "", null ,"/t/n"都是false "abc"才是true
        if(StrUtil.isNotBlank(jsonStr)){
            //3.存在 返回数据
            R r = JSONUtil.toBean(jsonStr, type);
            return r;
        }
        //判断是否是空值 此时shopJson可能是 "", null ,"/t/n"三种情况
        if(jsonStr != null ){
            //此时shopJson=="" 是我们缓存的空对象
            return null;
        }
        //4.此时shopJson为null 查询数据库
        //解决缓存击穿问题 - 基于互斥锁
        //4.1 获取互斥锁 - redis的String类型的setnx命令可以实现，因为它只有当这个值不存在的时候才能添加
        //如：setnx lock 1 -- 表示获取了锁      其他线程要setnx的时候不行
        //   del lock     --删除这个key，即释放锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean flag = tryLock(lockKey);
        R r= null;
        try {
            //4.2判断是否获取锁
            if(!flag){
                //4.3没有获取锁 - 休眠一段时间后重新查询商店缓存
                Thread.sleep(50);
                return queryWithMutex(prefKey,id,type,dbCallback,time,timeUnit); //递归再查询查询店铺缓存
            }
            //4.4获取到锁 - 进行数据查询 并写入缓存，返回数据，释放锁
            r = dbCallback.apply(id);
            if(r == null){
                //5.数据库不存在 返回错误
                //缓存空对象 2分钟超时
                set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //6.数据库存在 写入redis，添加60分钟的超时时间 返回数据
            set(key,JSONUtil.toJsonStr(r),time,timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放锁
            unLock(lockKey);
        }
        return r;
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
}
