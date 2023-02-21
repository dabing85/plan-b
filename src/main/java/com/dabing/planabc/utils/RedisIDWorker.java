package com.dabing.planabc.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIDWorker {
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 起始时间戳 - 2023-1-1-0-0-0
     */
    private static final long START_STAMP= 1672531200L;
    /**
     * 序列号位数
     */
    private static final long SEQUENCE_BIT= 32L;

    public RedisIDWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成全局唯一id
     * @param keyPrefix 业务前缀key
     * @return
     */
    public long nextId(String keyPrefix){
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long currentSec = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp=currentSec-START_STAMP;

        //2.生成序列号
        String s = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long increment = stringRedisTemplate.opsForValue().increment("incr:"+keyPrefix+":"+s);

        //3.拼接返回
        return timeStamp << SEQUENCE_BIT | increment;
    }

    public static void main(String[] args) {
        LocalDateTime time =LocalDateTime.of(2023,1,1,0,0,0);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second = " + second);
    }
}

