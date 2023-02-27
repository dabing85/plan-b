package com.dabing.planabc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Voucher;

/**
* @author 22616
* @description 针对表【tb_voucher】的数据库操作Service
* @createDate 2022-12-16 13:19:58
*/
public interface VoucherService extends IService<Voucher> {

    Result addSeckillVoucher(Voucher voucher);

    Result queryVoucherByShopId(Long shopId);
}
