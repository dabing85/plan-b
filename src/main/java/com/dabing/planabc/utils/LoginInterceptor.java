package com.dabing.planabc.utils;

import cn.hutool.core.bean.BeanUtil;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器 - 校验登录状态
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 从session中获取用户
        System.out.println("执行了登录拦截器");
        HttpSession session = request.getSession();
        User user = (User)session.getAttribute("user");
        //2.判断用户是否存在
        if(user==null){
            //3.不存在 拦截
            //状态码401表示
            response.setStatus(401);
            return false;
        }
        //4.存在 保存到ThreadLocal并放行
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        UserHolder.setUser(userDTO);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.remoteUser();
    }
}
