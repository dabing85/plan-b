package com.dabing.planabc.controller;

import cn.hutool.core.util.StrUtil;
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
        return shopService.queryShopById(id);
    }

    /**
     * 更新店铺信息 - 删除缓存
     * @param shop
     * @return
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop){
        return shopService.updateShop(shop);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
