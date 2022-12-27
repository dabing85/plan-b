package com.dabing.planabc.utils;

import cn.hutool.core.bean.BeanUtil;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dabing.planabc.utils.RedisConstants.LOGIN_USER_KEY;
import static com.dabing.planabc.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 登录拦截器 - 校验登录状态
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.从ThreadLocal中获取用户
        if(UserHolder.getUser()==null){
            //2.不存在 拦截
            response.setStatus(401);
            return false;
        }
        //3.放行
        return true;
    }
}
