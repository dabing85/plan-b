package com.dabing.planabc.controller;

import com.dabing.planabc.dto.Result;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.RegexUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @RequestMapping("/code")
    public Result sendCode(@RequestParam(value = "phone" )String phone, HttpSession session){
        return userService.sendCode(phone,session);
    }
}
