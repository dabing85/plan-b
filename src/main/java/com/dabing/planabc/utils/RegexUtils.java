package com.dabing.planabc.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 做正则校验的工具类
 */
public class RegexUtils {

    /**
     * 校验是非无效手机格式
     * @param phone 手机号
     * @return  true：无效手机号  false：不是无效的手机号
     */
    public static boolean isPhoneInvalid(String phone){
        return mismatch(phone,RegexPatterns.PHONE_REGEX);
    }

    /**
     * 是否是无效邮箱格式
     * @param email 要校验的邮箱
     * @return true:无效邮箱，false：不是无效邮箱
     */
    public static boolean isEmailInvalid(String email){
        return mismatch(email, RegexPatterns.EMAIL_REGEX);
    }

    /**
     * 是否是无效验证码格式
     * @param code 要校验的验证码
     * @return true:无效验证码，false：不是无效
     */
    public static boolean isCodeInvalid(String code){
        return mismatch(code, RegexPatterns.VERIFY_CODE_REGEX);
    }

    /**
     * 校验是否不符合正则表达式
     * @param str
     * @param regex
     * @return true 不符合正则
     */
    private static boolean mismatch(String str, String regex) {
        if(StrUtil.isBlank(str)){
            return true;
        }
        return !str.matches(regex);
    }

}
