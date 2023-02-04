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

    Result queryShopByType(Integer typeId, Integer current);

    Result queryShopById(Long id);

    Result updateShop(Shop shop);
}
