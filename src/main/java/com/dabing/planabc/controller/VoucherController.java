package com.dabing.planabc.controller;

import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Voucher;
import com.dabing.planabc.service.VoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private VoucherService voucherService;

    @GetMapping("/list/{id}")
    public Result queryVoucherByShopId(@PathVariable("id") Long id){
        List<Voucher> vouchers = voucherService.query().eq("shop_id", id).list();
        return Result.ok(vouchers);
    }

}
