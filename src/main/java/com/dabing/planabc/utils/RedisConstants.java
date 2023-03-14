package com.dabing.planabc.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY="login:code:";    //登录验证码
    public static final long LOGIN_CODE_TTL=2L;
    public static final String LOGIN_USER_KEY="login:token:";   //用户token
    public static final long LOGIN_USER_TTL=60L;
    public static final String CACHE_SHOP_KEY="cache:shop:";    //店铺缓存
    public static final long CACHE_SHOP_TTL=60L;
    public static final long CACHE_NULL_TTL=2L;
    public static final String LOCK_SHOP_KEY="lock:shop:";    //店铺互斥锁
    public static final long LOCK_SHOP_TTL=10L;               //互斥锁有效10秒
    public static final String SECKILL_STOCK_KEY="seckill:stock:";


}
