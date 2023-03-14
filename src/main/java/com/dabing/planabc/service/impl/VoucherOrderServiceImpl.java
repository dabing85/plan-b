package com.dabing.planabc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.VoucherOrder;
import com.dabing.planabc.mapper.VoucherOrderMapper;
import com.dabing.planabc.service.SeckillVoucherService;
import com.dabing.planabc.service.VoucherOrderService;
import com.dabing.planabc.utils.RedisIDWorker;
import com.dabing.planabc.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
* @author 22616
* @description 针对表【tb_voucher_order】的数据库操作Service实现
* @createDate 2022-12-16 13:20:03
*/
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
    implements VoucherOrderService {

    @Resource
    private SeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIDWorker redisIDWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;  //redisson分布式锁

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;   //判断用户是否有秒杀条件lua脚本
    static {
        SECKILL_SCRIPT =new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    private BlockingQueue<VoucherOrder> ordersTask=new ArrayBlockingQueue<>(1024*1024); //订单阻塞队列

    //开启处理订单的线程
    private static final ExecutorService EXECUTOR_ORDER_SERVICE= Executors.newSingleThreadExecutor();//单线程
    //在这个类初始化完成之后要开始拿去订单任务进行下单操作
    @PostConstruct
    private void init(){
        EXECUTOR_ORDER_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        VoucherOrder voucherOrder=ordersTask.take(); //获取阻塞队列的头部元素，没有则等待
                        proxy.createVoucherOrder(voucherOrder);
                    } catch (Exception e) {
                        log.error("处理订单异常",e);
                    }
                }
            }
        });
    }

    private VoucherOrderService proxy;
    @Override
    public Result seckillVoucher(Long voucherId){
        //1.执行lua脚本
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
                voucherId.toString(), userId.toString());
        //2.判断结果是否为0
        int i = result.intValue();
        //3.结果不为0
        if(i != 0){
            return Result.fail( i==1 ? "库存不足":"请勿重复下单");
        }
        //4.结果为0
        long orderId = redisIDWorker.nextId("order");
        //TODO 5.将用户、优惠券信息等保存到阻塞队列中
        VoucherOrder voucherOrder = new VoucherOrder();
        //5.1.订单id
        voucherOrder.setId(orderId);
        //5.2.用户id
        voucherOrder.setUserId(userId);
        //5.3.优惠券id
        voucherOrder.setVoucherId(voucherId);
        //5.4.保存订单信息到阻塞队列
        ordersTask.add(voucherOrder);
        //获取代理，用户开启下订单事务
        proxy = (VoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

//    @Override
//    public Result seckillVoucher(Long voucherId){
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        //1. 判断秒杀是否开始
//        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀还没开始！");
//        }
//        //2. 判断秒杀是否已经结束
//        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束了！");
//        }
//        //3. 判断库存是否充足
//        if(seckillVoucher.getStock() < 1){
//            return Result.fail("库存不足！");
//        }
//        //4. 创建订单
//        Long userId = UserHolder.getUser().getId();
//        //仅锁user对象
////        synchronized (userId.toString().intern()){
////            //事务失效 获取代理对象（事务）
////            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
////            return proxy.createVoucherOrder(voucherId);
////        }
//        //不使用synchronized上锁
//        //使用分布式锁
////        SimpleRedisLock simpleRedisLock=new SimpleRedisLock("order:"+userId,stringRedisTemplate);
////        boolean isLock = simpleRedisLock.tryLock(10l, TimeUnit.SECONDS);//10秒送自动释放锁
//
//        //使用redisson
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = lock.tryLock();
//
//
//        if(!isLock){
//            return Result.fail("限制一人一单！");
//        }
//        try {
//            //调用本类方法，事务失效
//            //获取代理对象（事务）
//            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
////            simpleRedisLock.unLock();
//            lock.unlock();
//        }
//    }

//    @Transactional
//    public Result createVoucherOrder(Long voucherId){
//        //4. 一人一单
//        Long userId = UserHolder.getUser().getId();
//        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
//        if(count > 0){
//            return Result.fail("用户已经购买过一次！");
//        }
//
//        //5. 开始下单
//        //5.1 库存-1
//        boolean success = seckillVoucherService.update()
//                .setSql("stock = stock -1 ")    //set stock = stock-1
//                .eq("voucher_id", voucherId).gt("stock",0) //where id=? and stock > 0
//                .update();
//        //5.2 生成订单
//        if(!success){
//            return Result.fail("库存不足！");
//        }
//        VoucherOrder voucherOrder = new VoucherOrder();
//        long orderId = redisIDWorker.nextId("order");
//        //1.订单id
//        voucherOrder.setId(orderId);
//        //2.用户id
//        voucherOrder.setUserId(UserHolder.getUser().getId());
//        //3.优惠券id
//        voucherOrder.setVoucherId(voucherId);
//        //4.保存订单
//        save(voucherOrder);
//        //6. 返回订单号
//        return Result.ok(orderId);
//    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder){
        //一人一单
        Long userId = voucherOrder.getUserId();
        long voucherId=voucherOrder.getVoucherId();
        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if(count > 0){
            log.error("用户已经购买过一次！");//理论上没有这个问题，作为兜底
        }
        //扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1 ")    //set stock = stock-1
                .eq("voucher_id", voucherId).gt("stock",0) //where id=? and stock > 0
                .update();

        if(!success){
            log.error("库存不足！"); //理论上不会出现这个问题
        }
        //保存订单
        save(voucherOrder);
    }
}




