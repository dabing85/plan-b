package com.dabing.planabc.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.LoginFormDTO;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.User;
import com.dabing.planabc.mapper.UserMapper;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.RegexUtils;
import com.dabing.planabc.utils.SystemConstants;
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
            return Result.fail("无效手机号!");
        }
        //2.生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);

        //3.保存随机码到session
        session.setAttribute("code",code);

        //4.发送随机码到用户手机
        log.debug("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginFormDto, HttpSession session) {
        //1.校验无效手机号
        String phone = loginFormDto.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("无效手机号!");
        }
        //2.校验验证码是否正确
        String code = loginFormDto.getCode();
        String sessionCode = (String)session.getAttribute("code");
        if(!sessionCode.equals(code)){
            return Result.fail("验证码错误！");
        }
        //3.查询数据库是否有该用户
        User user = query().eq("phone", phone).one();
        if(user==null){
            //3.1 没有 注册
            user=createUserWithPhone(phone);
        }

        //4.登录并保存用户信息到session
        session.setAttribute("user",user);
        return Result.ok();
    }

    /**
     * 用手机号码注册一个新用户
     * @param phone
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user=new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}




