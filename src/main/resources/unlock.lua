  --key "lock:"+name  参数key[1]
  --获取锁信息
  local value = redis.call('GET',KEYS[1])
  --获取线程信息 UUID+threadId arg[1]
  if(value == ARGV[1]) then
    --一致 释放锁
    return redis.call('DEL',KEYS[1])
  end
  --不一致 直接返回
  return 0;
