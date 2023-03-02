package com.dabing.planabc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.SeckillVoucher;
import com.dabing.planabc.entity.VoucherOrder;
import com.dabing.planabc.mapper.VoucherOrderMapper;
import com.dabing.planabc.service.SeckillVoucherService;
import com.dabing.planabc.service.VoucherOrderService;
import com.dabing.planabc.utils.RedisIDWorker;
import com.dabing.planabc.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
* @author 22616
* @description 针对表【tb_voucher_order】的数据库操作Service实现
* @createDate 2022-12-16 13:20:03
*/
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
    implements VoucherOrderService {

    @Resource
    private SeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIDWorker redisIDWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //1. 判断秒杀是否开始
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀还没开始！");
        }
        //2. 判断秒杀是否已经结束
        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束了！");
        }
        //3. 判断库存是否充足
        if(seckillVoucher.getStock() < 1){
            return Result.fail("库存不足！");
        }
        //4. 开始下单
        //4.1 库存-1
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1 ")
                .eq("voucher_id", voucherId).update();
        //4.2 生成订单
        if(!success){
            return Result.fail("库存不足！");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIDWorker.nextId("order");
        //1.订单id
        voucherOrder.setId(orderId);
        //2.用户id
        voucherOrder.setUserId(UserHolder.getUser().getId());
        //3.优惠券id
        voucherOrder.setVoucherId(voucherId);
        //4.保存订单
        save(voucherOrder);
        //5. 返回订单号
        return Result.ok(orderId);
    }
}




