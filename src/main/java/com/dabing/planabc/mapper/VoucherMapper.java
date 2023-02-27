package com.dabing.planabc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dabing.planabc.entity.Voucher;

import java.util.List;

/**
* @author 22616
* @description 针对表【tb_voucher】的数据库操作Mapper
* @createDate 2022-12-16 13:19:58
* @Entity generator.domain.Voucher
*/
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherByShopId(Long shopId);
}




