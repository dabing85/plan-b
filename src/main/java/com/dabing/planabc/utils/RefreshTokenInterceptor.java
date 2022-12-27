package com.dabing.planabc.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dabing.planabc.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dabing.planabc.utils.RedisConstants.LOGIN_USER_KEY;
import static com.dabing.planabc.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 刷新token拦截器 - 拦截所有路径
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("执行了刷新token拦截器");
        //1.1获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        //1.2从redis中获取hashmap
        String key = LOGIN_USER_KEY+token;
        //1.3讲hashmap转换成对象
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(key);
        // 2.判断用户是否存在
        if (map.isEmpty()) {
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        //4.存在 保存到ThreadLocal并放行
        UserHolder.setUser(userDTO);
        //5.刷新token有效期
        stringRedisTemplate.expire(key,LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.remoteUser();
    }
}
