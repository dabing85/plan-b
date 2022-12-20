package com.dabing.planabc.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Shop;
import com.dabing.planabc.service.ShopService;
import com.dabing.planabc.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/shop")
public class ShopController {
    @Resource
    private ShopService shopService;

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId    商铺类型
     * @param current   页码
     * @return  商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current",defaultValue = "1") Integer current
    ){
        Page<Shop> page = shopService.query()
                .eq("type_id", typeId)
                .orderByAsc("id")
                .page(new Page<>(current,SystemConstants.DEFAULT_PAGE_SIZE));
        List<Shop> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id")Long id){
        return Result.ok(shopService.getById(id));
    }
}
