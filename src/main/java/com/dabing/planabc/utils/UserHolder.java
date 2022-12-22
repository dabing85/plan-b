package com.dabing.planabc.utils;

import com.dabing.planabc.dto.UserDTO;

/**
 * 用于基于session登录的保存用户信息的ThreadLocal
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl =new ThreadLocal<>();
    public static void setUser(UserDTO userDTO){
        tl.set(userDTO);
    }
    public static UserDTO getUser(){
        return tl.get();
    }
    public static void remoteUser(){
        tl.remove();
    }

}
