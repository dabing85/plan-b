package com.dabing.planabc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Follow;

/**
* @author 22616
* @description 针对表【tb_follow】的数据库操作Service
* @createDate 2022-12-16 13:19:22
*/
public interface FollowService extends IService<Follow> {

    Result follow(Long id, Boolean isFollow);

    Result isFollow(Long id);
}
