package com.dabing.planabc.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    private static final String PREFIX_KEY="lock:";
    private static final String PREFIX_VALUE = UUID.randomUUID().toString(true)+"-"; //避免集群有相同的线程id

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) {
        String threadId=PREFIX_VALUE+Thread.currentThread().getId();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(PREFIX_KEY + name, threadId , timeout, unit);
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public void unLock() {
        String threadId=PREFIX_VALUE+Thread.currentThread().getId();
        String value = stringRedisTemplate.opsForValue().get(PREFIX_KEY+name);
        if(threadId.equals(value)){  //防误删
            stringRedisTemplate.delete(PREFIX_KEY + name);
        }
    }
}
