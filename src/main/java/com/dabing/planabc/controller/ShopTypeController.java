package com.dabing.planabc.controller;

import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.ShopType;
import com.dabing.planabc.service.ShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private ShopTypeService shopTypeService;

    @GetMapping("/list")
    public Result queryTypeList() {
        List<ShopType> typeList = shopTypeService
                .query().orderByAsc("sort").list();
        return Result.ok(typeList);
    }


}
