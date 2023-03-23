package com.dabing.planabc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.LoginFormDTO;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.User;
import com.dabing.planabc.mapper.UserMapper;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.RegexUtils;
import com.dabing.planabc.utils.SystemConstants;
import com.dabing.planabc.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dabing.planabc.utils.RedisConstants.*;

/**
* @author 22616
* @description 针对表【tb_user】的数据库操作Service实现
* @createDate 2022-12-16 13:19:46
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.判断手机号是否有效
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("无效手机号!");
        }
        //2.生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);

        //3.保存随机码到session
        //session.setAttribute("code",code);
        //修改为 - 保存到redis中，key为 login:code+手机号码 并设置有效期2分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

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
        //String sessionCode = (String)session.getAttribute("code");
        //改为从redis获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(!cacheCode.equals(code)){
            return Result.fail("验证码错误！");
        }
        //3.查询数据库是否有该用户
        User user = query().eq("phone", phone).one();
        if(user==null){
            //3.1 没有 注册
            user=createUserWithPhone(phone);
        }

        //4.登录并保存用户信息到session
        //session.setAttribute("user",user);
        //改为存储到redis中 ，key = login:token:随机码
        //4.1 生成随机token ，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //4.2 将user对象转换为hashmap对象
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)-> fieldValue.toString()));
        System.out.println("用户信息： " + map);
        //4.3 讲hashMap对象保存到redis
        String key = LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(key,map);
        //4.4 设置有效期
        stringRedisTemplate.expire(key,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2. 获取当天日期信息
        LocalDateTime now = LocalDateTime.now();
        String month= now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        //3. 拼接key
        String key = SIGN_USER_KEY+userId+month;
        //4. 获取当天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //5. 将签到信息保存到bitmap中 SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        Long userId = UserHolder.getUser().getId();
        //1.获取当天信息
        LocalDateTime now = LocalDateTime.now();
        String subPrefix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        //2.拼接key
        String key=SIGN_USER_KEY+userId+subPrefix;
        //3.获取当天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //4.获取本月到今天为止的所有签到数据？返回一个10进制数字  BITFIELD key GET u[dayOfMonth] 0
        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));
        //5.遍历数据
        if(result==null || result.isEmpty()){
            //没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == null || num==0){
            return Result.ok(0);
        }
        int count=0;
        while(true){
            //从后往前遍历，跟1相与，判断是否为0
            if((num & 1)==0){
                //为0，返回结果
                break;
            }else {
                //为1，count++
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }

        return Result.ok(count);
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




