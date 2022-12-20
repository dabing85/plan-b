package com.dabing.planabc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.entity.SeckillVoucher;
import com.dabing.planabc.mapper.SeckillVoucherMapper;
import com.dabing.planabc.service.SeckillVoucherService;
import org.springframework.stereotype.Service;

/**
* @author 22616
* @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Service实现
* @createDate 2022-12-16 13:19:28
*/
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher>
    implements SeckillVoucherService {

}




