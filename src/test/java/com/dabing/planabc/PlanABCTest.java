package com.dabing.planabc;

import com.dabing.planabc.entity.Shop;
import com.dabing.planabc.service.impl.ShopServiceImpl;
import com.dabing.planabc.utils.RedisIDWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.dabing.planabc.utils.RedisConstants.SHOP_GEO_KEY;

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
    @Resource
    private StringRedisTemplate stringRedisTemplate;
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

    /**
     * 加载店铺地理位置信息到geo
     */
    @Test
    public void testLoadShop(){
        //直接遍历导入也可以，但是老师的写法比较优雅复杂
//        List<Shop> list = shopService.query().list();
//        String key="geo:shopType:";
//        for (Shop shop : list) {
//            // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
//            stringRedisTemplate.opsForGeo().add(key+shop.getTypeId(),new Point(shop.getX(), shop.getY()),shop.getId().toString());
//        }

        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
