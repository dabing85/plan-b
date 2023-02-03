package com.dabing.planabc.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.ShopType;
import com.dabing.planabc.mapper.ShopTypeMapper;
import com.dabing.planabc.service.ShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
* @author 22616
* @description 针对表【tb_shop_type】的数据库操作Service实现
* @createDate 2022-12-16 13:19:40
*/
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType>
    implements ShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        //1. 查询缓存
        String shopType = stringRedisTemplate.opsForValue().get("shopType");
        //2.判断是否存在数据
        if(StrUtil.isNotBlank(shopType)){
            //3.存在 转换成list返回
            List<ShopType> shopTypes = JSONUtil.toList(shopType, ShopType.class);
            return Result.ok(shopTypes);
        }
        //4.不存在 查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if(shopTypes == null){
            //5.数据库不存在 返回错误
            return Result.fail("店铺分类不存在！");
        }
        //6.数据库存在   返回 更新到缓存
        stringRedisTemplate.opsForValue().set("shopType",JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }
}




