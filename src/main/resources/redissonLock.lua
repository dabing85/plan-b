--Redisson可重入锁原理
--获取锁的lua脚本
local key = KEYS[1]; -- 锁的key
local threadId = ARGV[1]; -- 线程的唯一标识
local releaseTime = ARGV[2]; -- 锁自动释放时间

--判断是否存在
if(redis.call('exists',key)==0) then
    --不存在，获取锁
    redis.call('hset',key,threadId,'1');
    --设置有效期
    redis.call('expire',key,releaseTime);
    return 1;   --返回结果
end;

--锁已经存在，判断threadId是否为自己的
if(redis.call('hexists',key,threadId)==1) then
    --是自己线程，重入锁+1
    redis.call('hincrby',key,threadId,'1');
    --设置有效期
    redis.call('expire',key,releaseTime);
    return 1;   --返回结果
end;
--其他说明说明获取锁不是自己的，获取锁失败
return 0;



--源码里的lua脚本
if (redis.call('exists', KEYS[1]) == 0) then
    redis.call('hincrby', KEYS[1], ARGV[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end;
if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then
    redis.call('hincrby', KEYS[1], ARGV[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end;
return redis.call('pttl', KEYS[1]);
