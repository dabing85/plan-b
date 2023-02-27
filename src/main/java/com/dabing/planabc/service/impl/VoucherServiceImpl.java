package com.dabing.planabc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.SeckillVoucher;
import com.dabing.planabc.entity.Voucher;
import com.dabing.planabc.mapper.VoucherMapper;
import com.dabing.planabc.service.SeckillVoucherService;
import com.dabing.planabc.service.VoucherService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
* @author 22616
* @description 针对表【tb_voucher】的数据库操作Service实现
* @createDate 2022-12-16 13:19:58
*/
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher>
    implements VoucherService {

    @Resource
    private SeckillVoucherService seckillVoucherService;

    @Override
    public Result addSeckillVoucher(Voucher voucher) {
        save(voucher);
        SeckillVoucher seckillVoucher=new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setCreateTime(voucher.getCreateTime());
        seckillVoucher.setUpdateTime(voucher.getUpdateTime());
        seckillVoucherService.save(seckillVoucher);
        return Result.ok(voucher.getId());
    }

    @Override
    public Result queryVoucherByShopId(Long shopId) {
//        List<Voucher> voucherList = query().eq("shop_id", shopId).list();
//        List<Voucher> vouchers= new ArrayList<>();
//        //遍历查询秒杀券
//        for(Voucher voucher:voucherList){
//            int type = voucher.getType();
//            //普通优惠券
//            if(type == 0){
//                vouchers.add(voucher);
//                continue;
//            }
//            //秒杀优惠券
//            SeckillVoucher seckillVoucher=seckillVoucherService.query().eq("voucher_id",voucher.getId()).one();
//            voucher.setBeginTime(seckillVoucher.getBeginTime());
//            voucher.setEndTime(seckillVoucher.getEndTime());
//            voucher.setStock(seckillVoucher.getStock());
//            vouchers.add(voucher);
//        }
        //使用sql实现
        List<Voucher> vouchers=getBaseMapper().queryVoucherByShopId(shopId);
        return Result.ok(vouchers);
    }
}




