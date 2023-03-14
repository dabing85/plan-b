package com.dabing.planabc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.VoucherOrder;

/**
* @author 22616
* @description 针对表【tb_voucher_order】的数据库操作Service
* @createDate 2022-12-16 13:20:03
*/
public interface VoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
