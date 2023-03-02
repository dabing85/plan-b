package com.dabing.planabc.controller;

import com.dabing.planabc.dto.Result;
import com.dabing.planabc.service.VoucherOrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private VoucherOrderService voucherOrderService;

    @PostMapping("/seckill/{voucherId}")
    public Result seckillVoucher(@PathVariable Long voucherId){
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
