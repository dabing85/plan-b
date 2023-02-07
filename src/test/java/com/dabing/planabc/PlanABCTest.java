package com.dabing.planabc;

import com.dabing.planabc.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class PlanABCTest {

    @Resource
    private ShopServiceImpl shopService;

    @Test
    public void saveShop2CacheTest(){
        shopService.saveShop2Cache(1l);
    }
}
