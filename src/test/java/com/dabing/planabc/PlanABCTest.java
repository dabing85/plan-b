package com.dabing.planabc;

import com.dabing.planabc.service.impl.ShopServiceImpl;
import com.dabing.planabc.utils.RedisIDWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class PlanABCTest {

    /**
     * 线程池
     */
    private ExecutorService executorService=Executors.newFixedThreadPool(500);

    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIDWorker redisIDWorker;

    @Test
    public void saveShop2CacheTest(){
        shopService.saveShop2Cache(1l);
    }

    @Test
    public void testRedisIdWorker() throws InterruptedException {
        CountDownLatch latch=new CountDownLatch(300);
        Runnable task = ()->{
            for (int i = 0; i < 100; i++) {
                long orderId = redisIDWorker.nextId("order");
                System.out.println("orderId = " + orderId);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("花费时长："+(end-begin));

    }
}
