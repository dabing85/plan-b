package com.dabing.planabc.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;


import java.util.Collections;
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
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) {
        String threadId=PREFIX_VALUE+Thread.currentThread().getId();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(PREFIX_KEY + name, threadId , timeout, unit);
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public void unLock() {
//        String threadId=PREFIX_VALUE+Thread.currentThread().getId();
//        String value = stringRedisTemplate.opsForValue().get(PREFIX_KEY+name);
//        if(threadId.equals(value)){  //防误删
//            stringRedisTemplate.delete(PREFIX_KEY + name);
//        }

        //使用lua脚本
        String threadId=PREFIX_VALUE+Thread.currentThread().getId();
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(PREFIX_KEY + name),threadId);
    }
}
