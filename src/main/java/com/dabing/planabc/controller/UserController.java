package com.dabing.planabc.controller;

import cn.hutool.core.bean.BeanUtil;
import com.dabing.planabc.dto.LoginFormDTO;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.User;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.RegexUtils;
import com.dabing.planabc.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginFormDto,HttpSession session){
        return userService.login(loginFormDto,session);
    }

    /**
     * 校验用户登录状态
     * @return
     */
    @GetMapping("/me")
    public Result me(){
        //获取用户登录状态并返回
        UserDTO userDTO = UserHolder.getUser();
        return Result.ok(userDTO);
    }


    @GetMapping("/info/{id}")
    public Result getUserInfo(@PathVariable("id") Long id ){
        User user = userService.getById(id);
        if(user==null){
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }
}
