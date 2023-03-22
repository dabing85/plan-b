package com.dabing.planabc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Shop;

/**
* @author 22616
* @description 针对表【tb_shop】的数据库操作Service
* @createDate 2022-12-16 13:19:34
*/
public interface ShopService extends IService<Shop> {

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);

    Result queryShopById(Long id);

    /**
     * 基于互斥锁方式解决缓存击穿问题
     */
    Result queryShopHCJCByLock(Long id);

    /**
     * 基于逻辑过期方式解决缓存击穿问题
     */
    Result queryShopHCJCByLogicalExpire(Long id);

    Result updateShop(Shop shop);
}
