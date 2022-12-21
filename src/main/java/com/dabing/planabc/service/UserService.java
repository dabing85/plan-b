package com.dabing.planabc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabing.planabc.dto.LoginFormDTO;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.User;

import javax.servlet.http.HttpSession;

/**
* @author 22616
* @description 针对表【tb_user】的数据库操作Service
* @createDate 2022-12-16 13:19:46
*/
public interface UserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginFormDto, HttpSession session);
}
