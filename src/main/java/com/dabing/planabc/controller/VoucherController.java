package com.dabing.planabc.controller;

import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Voucher;
import com.dabing.planabc.service.VoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private VoucherService voucherService;

    @GetMapping("/list/{shopId}")
    public Result queryVoucherByShopId(@PathVariable("shopId") Long shopId){
        return voucherService.queryVoucherByShopId(shopId);
    }

    /**
     * 新增秒杀券
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("/add/seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher){
        return voucherService.addSeckillVoucher(voucher);
    }

    /**
     * 新增普通券
     * @param voucher 优惠券信息
     * @return 优惠券id
     */
    @PostMapping("/add")
    public Result addVoucher(@RequestBody Voucher voucher){
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

}
