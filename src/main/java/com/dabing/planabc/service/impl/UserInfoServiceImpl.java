package com.dabing.planabc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.entity.UserInfo;
import com.dabing.planabc.mapper.UserInfoMapper;
import com.dabing.planabc.service.UserInfoService;
import org.springframework.stereotype.Service;

/**
* @author 22616
* @description 针对表【tb_user_info】的数据库操作Service实现
* @createDate 2022-12-16 13:19:52
*/
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService {

}




