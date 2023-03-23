🍈作者：王甜甜（dabing）

# 大冰点评笔记

一个redis的练手项目

github：

gitee：https://gitee.com/hedabing/plan-b.git

# 一、数据库

**表：**

blog - 博客
blog_comment - 博客评论
follow - 关注
seckill_voucher - 秒杀优惠券
shop - 店铺
shop_type - 店铺类型
user - 用户
user_info - 用户信息
voucher - 优惠券
voucher_order - 优惠券订单

![image-20221216155605668](https://image.dabing.cool/image/image-20221216155605668.png)

# 二、首页、项目初始化

前端是现有的，拿来即用，稍微修改一下。

增删改查使用mybatis-plus，直接用的mybatis-x插件生成的代码，也很快。

**正常五步**：

> 1. 建project  - plan-abc
> 2. pom文件  - parent、web、mysql、redis、mybatis-plus。。。
> 3. 主启动类
> 4. yml文件 - 端口、mysql、redis、日志
> 5. 业务类 - 增删改查mybatisx插件生成

完成基本的店铺、商铺的查询功能。

初始化之后大概长这样：![image-20221216173902651](https://image.dabing.cool/image/image-20221216173902651.png)

# 三、验证码登录

三步：**发送验证码并保存、校验验证码和手机号、校验登录状态**

## 1 - 基于Session短信登录

![image-20221217004215532](https://image.dabing.cool/image/image-20221217004215532.png)

先用session保存，未使用到redis。

<img src="https://image.dabing.cool/image/image-20221222215101595.png" alt="image-20221222215101595" style="zoom:50%;" />



## 2 - 基于redis短信登录

### **核心思路分析：**

每个tomcat中都有一份属于自己的session,假设用户第一次访问第一台tomcat，并且把自己的信息存放到第一台服务器的session中，但是第二次这个用户访问到了第二台tomcat，那么在第二台服务器上，肯定没有第一台服务器存放的session，所以此时 整个登录拦截功能就会出现问题，我们能如何解决这个问题呢？早期的方案是session拷贝，就是说虽然每个tomcat上都有不同的session，但是每当任意一台服务器的session修改时，都会同步给其他的Tomcat服务器的session，这样的话，就可以实现session的共享了

但是这种方案具有两个大问题

1、每台服务器中都有完整的一份session数据，服务器压力过大。

2、session拷贝数据时，可能会出现延迟

所以咱们后来采用的方案都是基于redis来完成，我们把session换成redis，redis数据本身就是共享的，就可以避免session共享的问题了

![image-20221222221215353](https://image.dabing.cool/image/image-20221222221215353.png)

反正就是之前保存再session的东西，换成保存在redis里共享的空间里，这样不管下一次请求哪个服务器都可以去redis取出来验证。

包括**验证码、用户信息** ，验证码直接使用String类型，用户信息可以用Hash结构

验证码可以使用phone作为key，用户信息可以生成一个随机的token作为key进行保存。

![image-20221227125625807](https://image.dabing.cool/image/image-20221227125625807.png)

### Redis代码：

```java
//验证码 - String
//保存到redis中，key为 login:code + 手机号码 并设置有效期2分钟
stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

//用户信息 - hash
//4.1 生成随机token ，作为登录令牌
String token = UUID.randomUUID().toString(true);
//4.2 将user对象转换为hashmap对象
UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
      CopyOptions.create()
      .setIgnoreNullValue(true)
      .setFieldValueEditor((fieldName,fieldValue)-> fieldValue.toString()));
//4.3 讲hashMap对象保存到redis
String key = LOGIN_USER_KEY+token;
stringRedisTemplate.opsForHash().putAll(key,map);
//4.4 设置有效期
stringRedisTemplate.expire(key,LOGIN_USER_TTL,TimeUnit.MINUTES);
```

### 优化问题：

用户也设置了有效期是60分钟，不管如何过了一个小时之后，用户登录信息就失效了，就得重新登录了。这样是不符合我们的需求的。

希望是用户在浏览页面的时候不会退出登录，只有用户不动，没有发出请求，60分钟后才过期失效。所以希望在用户每次发出请求的时候都刷新一次有效期，这样只要用户在浏览页面都不会失效了。

这个要求可以使用拦截器实现，我们原本有一个登录拦截器了，可以把刷新有效期放里面。但是还有一个问题的，不是所有路径都会触发登录拦截器的，例如商店查看、博客查看。

所以我们需要新添加一个token有效期刷新的拦截器，拦截所有路径，后才使用登录拦截器。实现如图：

![image-20221227230343419](https://image.dabing.cool/image/image-20221227230343419.png)

# 四、商户查询缓存

## 1. 添加商品缓存思路

标准的操作方式就是查询数据库之前先查询缓存，如果缓存数据存在，则直接从缓存中返回，如果缓存数据不存在，再查询数据库，然后将数据存入redis。

![image-20230128211343297](https://image.dabing.cool/image/image-20230128211343297.png)

使用String类型代码实现：

```java
//1. 查询缓存
String shopType = stringRedisTemplate.opsForValue().get("shopType");
//2.判断是否存在数据
if(StrUtil.isNotBlank(shopType)){
    //3.存在 转换成list返回
    List<ShopType> shopTypes = JSONUtil.toList(shopType, ShopType.class);
    return Result.ok(shopTypes);
}
//4.不存在 查询数据库
List<ShopType> shopTypes = query().orderByAsc("sort").list();
if(shopTypes == null){
    //5.数据库不存在 返回错误
    return Result.fail("店铺分类不存在！");
}
//6.数据库存在   返回 更新到缓存
stringRedisTemplate.opsForValue().set("shopType",JSONUtil.toJsonStr(shopTypes));
return Result.ok(shopTypes);
```

## 2. 缓存的更新策略

**缓存里的数据跟数据库的数据不一致**？-----更新数据时，删除缓存

综合来看的话，对于高一致性的需求来说，**要主动更新，就是更新数据库的时候，删除缓存（不之际）。先做更新数据库，再删除缓存数据。**缓存再加上一个超时时间进行兜底。

1. 查询数据 ： 先查询缓存，有数据直接返回，没有则查询数据库，将数据库数据同步到缓存中，返回数据，给缓存添加一个超时时间兜底。
2. 更新数据：更新数据库，删除缓存。

使用postman进行测试，其使用待熟悉

<img src="https://image.dabing.cool/image/image-20230204210328293.png" alt="image-20230204210328293" style="zoom: 33%;" />

## 3. 缓存穿透问题

`缓存穿透`，是指客户端请求的数据在缓存和数据库都不存在，这样缓存永远都不生效，这些请求都会打到数据库。

比如用一个不存在的用户id获取用户信息，不论缓存还是数据库都没有，若黑客利用此漏洞进行攻击可能压垮数据库。

**解决方案**：

1. **缓存空对象**

就是如果请求数据库也没有这个对象的话，就缓存一个null到redis，这样下次再访问这个key的时候，就返回一个null就不会去访问数据库。但是这样会产生垃圾数据，消耗内存空间，短期不一致问题。所以一般会设置一个比较段的过期时间，最长不超过五分钟。

2. **布隆过滤**  - 加一层布隆过滤器

那为什么布隆过滤器知道数据库里面有没有数据从而进行数据过滤呢？其实是将数据库的数据进行hash计算出一个值存储再布隆过滤器里，请求的时候如果拒绝了说明数据库里没有，如果通过了也不一定百分百有数据，因为这个计算也是会有一样的值的，会占位的。也是会有击穿的风险，但至少进行了一次过滤。

![image-20230204212232835](https://image.dabing.cool/image/image-20230204212232835.png)

这里选择缓存空对象的方式，布隆过滤实现起来比较复杂。原来的逻辑有一点点改变。

![image-20230204213236178](https://image.dabing.cool/image/image-20230204213236178.png)

**小总结：**

缓存穿透产生的原因是什么？

* 用户请求的数据在缓存中和数据库中都不存在，不断发起这样的请求，给数据库带来巨大压力

缓存穿透的解决方案有哪些？

* 缓存null值
* 布隆过滤
* 增强id的复杂度，避免被猜测id规律
* 做好数据的基础格式校验
* 加强用户权限校验
* 做好热点参数的限流

## 4. 缓存雪崩问题

缓存雪崩，是指同一时段大量的缓存key同时失效或者Redis服务宕机，导致大量请求到达数据库，带来巨大压力。

**解决方案**：

1. 将缓存失效时间分散开

   比如我们可以在原有的失效时间基础上增加一个随机值，比如1-5分钟随机，这样每一个缓存的过期时间的重复率就会降低，就很难引发集体失效的事件。

   那如果是redis宕机了？

2. 利用redis集群提高服务的可用性

3. 给缓存业务添加降级限流策略

4. 给业务添加多级缓存

## 5. 缓存击穿问题

`缓存击穿`，也叫**热点key**问题，就是一个**被高并发访问**并且**缓存重建业务较复杂**的key突然失效了，无数的请求访问会在瞬间给数据库带来巨大的冲击。

常见的**解决方案**有两种：

* 互斥锁 - 在查询数据库重建缓存的时候需要获取互斥锁进行运行，其他线程等待第一个重建缓存

* 逻辑过期 - 就是相当于热点的key不过期，但是当需要失效数据的时候手动逻辑过期数据

  ![image-20230205020014739](https://image.dabing.cool/image/image-20230205020014739.png)

### 5.1 基于互斥锁方式解决缓存击穿问题

逻辑如下：

![image-20230205020637228](https://image.dabing.cool/image/image-20230205020637228.png)

**代码实现**：

获取锁和释放锁：

```java
private boolean tryLock(String key) {
    Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
    return BooleanUtil.isTrue(flag);
}

private void unlock(String key) {
    stringRedisTemplate.delete(key);
}
```



```java
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
```

### 5.2 利用逻辑过期解决缓存击穿问题

**需求：修改根据id查询商铺的业务，基于逻辑过期方式来解决缓存击穿问题**

![image-20230205221828480](https://image.dabing.cool/image/image-20230205221828480.png)

## 6. 封装Redis工具类

基于StringRedisTemplate封装一个缓存工具类，满足下列需求：

* 方法1：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
* 方法2：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓

存击穿问题

* 方法3：根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
* 方法4：根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题

将逻辑进行封装

代码实现：

```java
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

```

# 五、优惠券秒杀

对于优惠券问题，面临着全局唯一id、秒杀下单、超卖问题、一人一单等问题

## 1. 全局唯一ID

订单id需要时全局唯一的，但不能使用数据库的自增id，因为自增id的规律性太明显，容易让用户猜测到订单的情况信息；第二个是单表有数据量限制，订单量是比较巨大的，那就需要分表了，分表之后各个单表是各自自增的，会有相同的id，那此时的订单号就不唯一了。

**解决方案：**

使用**全局ID生成器**，全局ID生成器是一种在分布式下用来生成全局唯一ID的工具，一般要满足下列特性：

1. 唯一性
2. 高可用
3. 高性能
4. 递增性（有利于数据库创建索引，插入数据）
5. 安全性

以下使用redis方式实现：redis的string类型的incr可以使用自增，满足1234点的条件

为了增加ID的安全性，我们可以不直接使用Redis自增的数值，而是拼接一些其它信息：

![1653363172079](https://image.dabing.cool/image/1653363172079.png)

采用的是long类型，8个字节。64个比特位。

ID的组成部分：符号位：1bit，永远为0

时间戳：31bit，以秒为单位，可以使用69年

序列号：32bit，秒内的计数器，支持每秒产生2^32个不同ID

但是注意redis的string的自增长也是有上限的，上限为2的64次方。所以不能用同一个key值进行自增长，怕是有一天会超过上限。所以key最好再拼接上当前日期，又具有当天订单量的统计的效果。

**代码实现：**

```java
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
```

**总结：**

全局唯一ID生成策略：

- UUID
- Redis自增
- snowflake算法
- 数据库自增

Redis自增ID策略：

符号位+时间戳+序列号

## 2. 优惠券秒杀下单

每个店铺都可以发布优惠券，分为平价券和特价券。平价券可以任意购买，而特价券需要秒杀抢购：

![1653365145124](https://image.dabing.cool/image/1653365145124.png)

tb_voucher：优惠券的基本信息，优惠金额、使用规则等
tb_seckill_voucher：优惠券的库存、开始抢购时间，结束抢购时间。特价优惠券才需要填写这些信息

平价卷由于优惠力度并不是很大，所以是可以任意领取

而代金券由于优惠力度大，所以像第二种卷，就得限制数量，从表结构上也能看出，特价卷除了具有优惠卷的基本信息以外，还具有库存，抢购时间，结束时间等等字段

完成普通优惠券、秒杀优惠券的新增功能✔

对于秒杀下单，粗略流程是：

![1653366238564](https://image.dabing.cool/image/1653366238564.png)

### 2.1 超卖问题

但是经过多线程并发测试，发现有超卖的线程安全问题：

<img src="https://image.dabing.cool/image/image-20230302181519291.png" alt="image-20230302181519291" style="zoom: 50%;" /> 

![image-20230302181606854](https://image.dabing.cool/image/image-20230302181606854.png)

假设线程1过来查询库存，判断出来库存大于1，正准备去扣减库存，但是还没有来得及去扣减，此时线程2过来，线程2也去查询库存，发现这个数量一定也大于1，那么这两个线程都会去扣减库存，最终多个线程相当于一起去扣减库存，此时就会出现库存的超卖问题。

![1653368335155](https://image.dabing.cool/image/1653368335155.png)

**课程中的使用方式：**

课程中的使用方式是没有像cas一样带自旋的操作，也没有对version的版本号+1 ，他的操作逻辑是在操作时，对版本号进行+1 操作，然后要求version 如果是1 的情况下，才能操作，那么第一个线程在操作后，数据库中的version变成了2，但是他自己满足version=1 ，所以没有问题，此时线程2执行，线程2 最后也需要加上条件version =1 ，但是现在由于线程1已经操作过了，所以线程2，操作时就不满足version=1 的条件了，所以线程2无法执行成功

![1653369268550](https://image.dabing.cool/image/1653369268550.png)



**修改代码方案一、**

VoucherOrderServiceImpl 在扣减库存时，改为：

```java
boolean success = seckillVoucherService.update()
            .setSql("stock= stock -1") //set stock = stock -1
            .eq("voucher_id", voucherId).eq("stock",voucher.getStock()).update(); //where id = ？ and stock = ?
```

以上逻辑的核心含义是：只要我扣减库存时的库存和之前我查询到的库存是一样的，就意味着没有人在中间修改过库存，那么此时就是安全的，但是以上这种方式通过测试发现会有很多失败的情况，失败的原因在于：在使用乐观锁过程中假设100个线程同时都拿到了100的库存，然后大家一起去进行扣减，但是100个人中只有1个人能扣减成功，其他的人在处理时，他们在扣减时，库存已经被修改过了，所以此时其他线程都会失败

**修改代码方案二、**

之前的方式要修改前后都保持一致，但是这样我们分析过，成功的概率太低，所以我们的乐观锁需要变一下，改成stock大于0 即可

```java
boolean success = seckillVoucherService.update()
            .setSql("stock= stock -1")
            .eq("voucher_id", voucherId).update().gt("stock",0); //where id = ? and stock > 0
```

**知识小扩展：**

针对cas中的自旋压力过大，我们可以使用Longaddr这个类去解决

Java8 提供的一个对AtomicLong改进后的一个类，LongAdder

大量线程并发更新一个原子性的时候，天然的问题就是自旋，会导致并发性问题，当然这也比我们直接使用syn来的好

所以利用这么一个类，LongAdder来进行优化

如果获取某个值，则会对cell和base的值进行递增，最后返回一个完整的值

![1653370271627](https://image.dabing.cool/image/1653370271627.png)



### 2.2 一人一单

需求：修改秒杀业务，要求同一个优惠券，一个用户只能下一单

具体操作逻辑如下：比如时间是否充足，如果时间充足，则进一步判断库存是否足够，然后再根据优惠卷id和用户id查询是否已经下过这个订单，如果下过这个订单，则不再下单，否则进行下单

![1653371854389](https://image.dabing.cool/image/1653371854389.png)

在扣减库存，生成订单之前要先进行一人一单的判断：

```java
// 5.一人一单逻辑
    // 5.1.用户id
    Long userId = UserHolder.getUser().getId();
    int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    // 5.2.判断是否存在
    if (count > 0) {
        // 用户已经购买过了
        return Result.fail("用户已经购买过一次！");
    }
```

但是它也同样有跟超卖一样，并发查询订单，存在并发线程安全问题，需要加锁。但是乐观锁比较适合更新数据，而现在是插入数据，所以我们需要使用悲观锁操作

这里的代码涉及到了spring框架事务失效，aop代理对象，synchronized锁对象等问题，需要额外去学习，代码如下：

```java
 //4. 创建订单
        Long userId = UserHolder.getUser().getId();
        //仅锁user对象
        synchronized (userId.toString().intern()){
            //事务失效 获取代理对象（事务）
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
```

```java
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.dabing.planabc.mapper")
@SpringBootApplication
public class PlanABCApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanABCApplication.class,args);
    }
}
```

```xml
		<dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
        </dependency>
```

## 3. 分布式锁

前面的synchronized所以已经完成了上锁的要求，但是如果这个项目不是单机，是集群环境，部署了多个tomcat，每个tomcat都有一个属于自己的jvm，那么在多个jvm中，synchronized是不共享的，还是会发生并发线程安全问题，所以需要找一个共享的空间，将互斥锁保存至该空间。

这种情况下，我们就需要使用分布式锁来解决这个问题。

![1653374044740](https://image.dabing.cool/image/1653374044740.png)

分布式锁：满足分布式系统或集群模式下多进程可见并且互斥的锁。

这里我们使用redis来实现，跟前面的解决缓存击穿问题使用互斥锁的原理是一样的，利用setnx方法进行加锁。

**代码见 - 4.0 分布式锁-简单待优化**

注：存入的value为uuid+线程id。其中uuid是用来区分不同服务器（JVM）的，线程id是用来区分相同jvm的不同线程的。实现所有线程都不相同。

### 3.1 锁误删问题

**代码见 - 4.1 分布式锁-防锁误删**

核心逻辑：在存入锁时，放入自己线程的标识，在删除锁时，判断当前这把锁的标识是不是自己存入的，如果是，则进行删除，如果不是，则不进行删除。

![1653387398820](https://image.dabing.cool/image/1653387398820.png)

虽然释放锁之前已经做了判断锁操作，但是由于判断锁和释放锁不是原子操作，他们是两步操作，如果在释放锁的时候发生了阻塞，以至于锁超时释放，其他线程乘虚而入，仍然会发生线程安全问题。

要想避免这个问题发生，需要判断锁和释放锁是原子操作即可。

### 3.2 使用lua脚本保证原子性

Redis提供了Lua脚本功能，在一个脚本中编写多条Redis命令，确保多条命令执行时的原子性。Lua是一种编程语言，它的基本语法大家可以参考网站：https://www.runoob.com/lua/lua-tutorial.html

**代码见 4.2分布式锁-使用lua脚本防误删**

但是，原子性的问题解决了，其实还有问题。如果redis提供了主从集群，redis的主机宕机了，线程A在主机上获取了锁，但是主还没来得及同步数据给从，主就宕机了，那么从变成主之后，别的线程B又可以在新的主redis上获取锁，还是会又线程安全问题。

这个问题是考虑到了redis的主机宕机数据未来得及同步的情况。

## 4. Redisson

## 5. Redis优化秒杀

![1653560986599](https://image.dabing.cool/image/1653560986599.png)

因为整个下单的操作是串行的，耗时比较久。可以类比餐厅服务员招待客人，接待客人做饭一步一步进行会消耗大量时间，不利于并发。但是如果将招待客人和做饭分开两个人来做，那就会快很多。

在这里，判断用户是否有秒杀资格和创建订单可以异步进行。判断了用户有秒杀资格后保存信息（类似吃饭小票）到队列中，直接返回订单id，在用户看来已经秒杀完成了。但是实际的创建订单操作可以异步进行，需要保证这个订单一定会成功。

![1653562234886](https://image.dabing.cool/image/1653562234886.png)

代码见：5.1 、5.2

# 六、消息队列-基于stream的消息队列

创建消费者组：
![1653577984924](E:/BaiduNetdiskDownload/黑马redis/02-实战篇/讲义/Redis实战篇.assets/1653577984924.png)
key：队列名称
groupName：消费者组名称
ID：起始ID标示，$代表队列中最后一个消息，0则代表队列中第一个消息
MKSTREAM：队列不存在时自动创建队列
其它常见命令：

**删除指定的消费者组**

```java
XGROUP DESTORY key groupName
```

**给指定的消费者组添加消费者**

```java
XGROUP CREATECONSUMER key groupname consumername
```

**删除消费者组中的指定消费者**

```java
XGROUP DELCONSUMER key groupname consumername
```

从消费者组读取消息：

```java
XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] ID [ID ...]
```

* group：消费组名称
* consumer：消费者名称，如果消费者不存在，会自动创建一个消费者
* count：本次查询的最大数量
* BLOCK milliseconds：当没有消息时最长等待时间
* NOACK：无需手动ACK，获取到消息后自动确认
* STREAMS key：指定队列名称
* ID：获取消息的起始ID：

">"：从下一个未消费的消息开始
其它：根据指定id从pending-list中获取已消费但未确认的消息，例如0，是从pending-list中的第一个消息开始

消费者监听消息的基本思路：

![1653578211854](https://image.dabing.cool/image/1653578211854.png)

# 七、达人探店

图片上传、发布笔记、点赞功能、查看笔记、点赞排行等功能详见6.代码。

# 八、好友关注

关注、取关、共同关注等功能详见7.的代码

## 1. 好友关注-Feed流

对于新型的Feed流的的效果：不需要我们用户再去推送信息，而是系统分析用户到底想要什么，然后直接把内容推送给用户，从而使用户能够更加的节约时间，不用主动去寻找。

![1653808993693](https://image.dabing.cool/image/1653808993693.png)

Feed流的实现有两种模式：

Feed流产品有两种常见模式：
**Timeline**：不做内容筛选，简单的按照内容发布时间排序，常用于好友或关注。例如朋友圈

* 优点：信息全面，不会有缺失。并且实现也相对简单
* 缺点：信息噪音较多，用户不一定感兴趣，内容获取效率低

**智能排序**：利用智能算法屏蔽掉违规的、用户不感兴趣的内容。推送用户感兴趣信息来吸引用户

* 优点：投喂用户感兴趣信息，用户粘度很高，容易沉迷
* 缺点：如果算法不精准，可能起到反作用
  本例中的个人页面，是基于关注的好友来做Feed流，因此采用Timeline的模式。该模式的实现方案有三种：

我们本次针对好友的操作，采用的就是Timeline的方式，只需要拿到我们关注用户的信息，然后按照时间排序即可

，因此采用Timeline的模式。该模式的实现方案有三种：

* 拉模式
* 推模式
* 推拉结合

**拉模式**：也叫做读扩散

该模式的核心含义就是：当张三和李四和王五发了消息后，都会保存在自己的邮箱中，假设赵六要读取信息，那么他会从读取他自己的收件箱，此时系统会从他关注的人群中，把他关注人的信息全部都进行拉取，然后在进行排序

优点：比较节约空间，因为赵六在读信息时，并没有重复读取，而且读取完之后可以把他的收件箱进行清楚。

缺点：比较延迟，当用户读取数据时才去关注的人里边去读取数据，假设用户关注了大量的用户，那么此时就会拉取海量的内容，对服务器压力巨大。

![1653809450816](https://image.dabing.cool/image/1653809450816.png)



**推模式**：也叫做写扩散。

推模式是没有写邮箱的，当张三写了一个内容，此时会主动的把张三写的内容发送到他的粉丝收件箱中去，假设此时李四再来读取，就不用再去临时拉取了

优点：时效快，不用临时拉取

缺点：内存压力大，假设一个大V写信息，很多人关注他， 就会写很多分数据到粉丝那边去

![1653809875208](https://image.dabing.cool/image/1653809875208.png)

**推拉结合模式**：也叫做读写混合，兼具推和拉两种模式的优点。

推拉模式是一个折中的方案，站在发件人这一段，如果是个普通的人，那么我们采用写扩散的方式，直接把数据写入到他的粉丝中去，因为普通的人他的粉丝关注量比较小，所以这样做没有压力，如果是大V，那么他是直接将数据先写入到一份到发件箱里边去，然后再直接写一份到活跃粉丝收件箱里边去，现在站在收件人这端来看，如果是活跃粉丝，那么大V和普通的人发的都会直接写入到自己收件箱里边来，而如果是普通的粉丝，由于他们上线不是很频繁，所以等他们上线时，再从发件箱里边去拉信息。

![1653812346852](https://image.dabing.cool/image/1653812346852.png)

**代码见 7.1-7.5**

# 九、附近商户

附近商户使用到的数据结构是Geospatial

## 1. 相关命令

```bash
命令：
   （1）存：geoadd
   （2）取：geopos
   （3）直线距离：geodist
   （4）指定中心、半径内的元素：georadius （6以后已废弃）、GEOSEARCH、GEOSEARCHSTORE
详见下列实例
```

* GEOADD：添加一个地理空间信息，包含：经度（longitude）、纬度（latitude）、值（member）
* GEODIST：计算指定的两个点之间的距离并返回
* GEOHASH：将指定member的坐标转为hash字符串形式并返回
* GEOPOS：返回指定member的坐标
* GEORADIUS：指定圆心、半径，找到该圆内包含的所有member，并按照与圆心之间的距离排序后返回。6.以后已废弃
* GEOSEARCH：在指定范围内搜索member，并按照与指定点之间的距离排序后返回。范围可以是圆形或矩形。6.2.新功能
* GEOSEARCHSTORE：与GEOSEARCH功能一致，不过可以把结果存储到一个指定的key。 6.2.新功能

**代码见8.1-8.2**

# 十、用户签到

用户签到可以使用bitmaps结构，

Redis提供了Bitmaps这个“数据类型”可以实现对位的操作：
（1）	Bitmaps本身不是一种数据类型， 实际上它就是字符串（key-value） ， 但是它可以对字符串的位进行操作。
（2）	Bitmaps单独提供了一套命令， 所以在Redis中使用Bitmaps和使用字符串的方法不太相同。 可以把Bitmaps想象成一个以位为单位的数组， 数组的每个单元只能存储0和1， 数组的下标在Bitmaps中叫做偏移量

Redis中是利用string类型数据结构实现BitMap，因此最大上限是512M，转换为bit则是 2^32个bit位。

![1653824498278](https://image.dabing.cool/image/1653824498278.png)

## 1. 相关命令

```bash
命令：
  （1）存：setbit
  （2）取：getbit
  （3）统计：bitcount
  （4）并/交/异或操作：bitop
详见下列很实例示范。
```

* SETBIT：向指定位置（offset）存入一个0或1
* GETBIT ：获取指定位置（offset）的bit值
* BITCOUNT ：统计BitMap中值为1的bit位的数量
* BITFIELD ：操作（查询、修改、自增）BitMap中bit数组中的指定位置（offset）的值
* BITFIELD_RO ：获取BitMap中bit数组，并以十进制形式返回
* BITOP ：将多个BitMap的结果做位运算（与 、或、异或）
* BITPOS ：查找bit数组中指定范围内第一个0或1出现的位置

**实现代码见9.1-9.3**

# 十一、UV统计

## 1. UV统计-HyperLogLog

首先我们搞懂两个概念：

* UV：全称Unique Visitor，也叫独立访客量，是指通过互联网访问、浏览这个网页的自然人。1天内同一个用户多次访问该网站，只记录1次。
* PV：全称Page View，也叫页面访问量或点击量，用户每访问网站的一个页面，记录1次PV，用户多次打开页面，则记录多次PV。往往用来衡量网站的流量。

通常来说UV会比PV大很多，所以衡量同一个网站的访问量，我们需要综合考虑很多因素，所以我们只是单纯的把这两个值作为一个参考值

UV统计在服务端做会比较麻烦，因为要判断该用户是否已经统计过了，需要将统计过的用户信息保存。但是如果每个访问的用户都保存到Redis中，数据量会非常恐怖，那怎么处理呢？

Hyperloglog(HLL)是从Loglog算法派生的概率算法，用于确定非常大的集合的基数，而不需要存储其所有值。相关算法原理大家可以参考：https://juejin.cn/post/6844903785744056333#heading-0
Redis中的HLL是基于string结构实现的，单个HLL的内存**永远小于16kb**，**内存占用低**的令人发指！作为代价，其测量结果是概率性的，**有小于0.81％的误差**。不过对于UV统计来说，这完全可以忽略。

![1653837988985](https://image.dabing.cool/image/1653837988985.png)

## 2. UV统计-测试百万数据的统计

**代码见10.1**

测试思路：我们直接利用单元测试，向HyperLogLog中添加100万条数据，看看内存占用和统计效果如何

![1653838053608](https://image.dabing.cool/image/1653838053608.png)

经过测试：我们会发生他的误差是在允许范围内，并且内存占用极小

# 待优化问题

1. ~~查询对应分类的店铺反复查询到重复数据 ，初步判断应该是前端的问题~~（mp的分页插件失效）
2. 了解函数式编程
3. 了解事务失效的几种情况（spring框架事务失效，aop代理对象，synchronized锁对象）

动态代理、AOP、事务、悲观锁、锁的范围。

有一个弹幕说，只要把类上的事务注解去掉就ok了、或者不要拆开、或者直接使用编程式事务，

4. ~~在集群的情况下，线程id是有可能会重复的，可以使用进程id+线程id生成全局唯一id~~
5. 查询关注用户feed流滚动分页前端重复发送请求，没有做防抖/节流