package com.dabing.planabc.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.User;
import com.dabing.planabc.mapper.UserMapper;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
* @author 22616
* @description 针对表【tb_user】的数据库操作Service实现
* @createDate 2022-12-16 13:19:46
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.判断手机号是否有效
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("请输入正确的手机号码");
        }
        //2.生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);

        //3.保存随机码到session
        session.setAttribute("code",code);

        //4.发送随机码到用户手机
        log.debug("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }
}




