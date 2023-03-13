--删除锁的lua脚本
local key= KEYS[1]; -- 锁的key
local threadId=ARGV[1]; -- 线程唯一标识
local releaseTime = ARGV[2]; -- 锁的自动释放时间
--判断是否为自己线程的锁
if(redis.call('hexists',key,threadId)==0) then
    --不是自己的锁,直接返回
    return nil;
end;

--是自己的锁，重入次数值-1
local count = redis.call('hincrby',key,threadId,-1);
--判断重入次数值是否为0
if(count>0) then
    --大于0说明不能释放锁，重置有效期然后返回
    redis.call('expire',key,releaseTime);
    return nil;
else
    redis.call('hdel',key);
    return nil;
end;

