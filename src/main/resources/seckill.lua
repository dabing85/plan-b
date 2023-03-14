--判断秒杀条件lua脚本

--1.参数
--1.1 优惠券id
local voucherId = ARGV[1]
--1.2 用户id
local userId=ARGV[2]
--1.3 订单id
local orderId=ARGV[3]

--2.数据key
--2.1 库存key
local stockKey = "seckill:stock:"..voucherId;
--2.2 订单key
local orderKey = "seckill:order:"..voucherId;

--3.业务逻辑
--3.1判断库存是否充足
if(tonumber(redis.call('get',stockKey))<=0) then
    return 1;
end
--3.2 判断是否一人一单
if(redis.call('sismember',orderKey,userId)==1) then
    return 2;
end
--3.3 符合下单条件
--3.3.1扣减库存
redis.call('incrby',stockKey,-1);
--3.3.2将用户保存到set集合中
redis.call('sadd',orderKey,userId);
--3.3.3将信息保存到stream消息队列中 XADD stream.orders * k1 v1 k2 v2 ..
redis.call('XADD','stream.orders','*','voucherId',voucherId,'userId',userId,'id',orderId);
return 0;