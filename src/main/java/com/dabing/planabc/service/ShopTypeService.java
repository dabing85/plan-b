package com.dabing.planabc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.ShopType;

/**
* @author 22616
* @description 针对表【tb_shop_type】的数据库操作Service
* @createDate 2022-12-16 13:19:40
*/
public interface ShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}
