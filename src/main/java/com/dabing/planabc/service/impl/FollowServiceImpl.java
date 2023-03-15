package com.dabing.planabc.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.Follow;
import com.dabing.planabc.mapper.FollowMapper;
import com.dabing.planabc.service.FollowService;
import com.dabing.planabc.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
* @author 22616
* @description 针对表【tb_follow】的数据库操作Service实现
* @createDate 2022-12-16 13:19:22
*/
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
    implements FollowService {

    @Override
    public Result follow(Long id, Boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return Result.fail("未登录");
        }
        Long userId = user.getId();
        //1.判断是关注还是取消
        if(BooleanUtil.isTrue(isFollow)){
            //关注，添加关注数据
            Follow follow=new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            save(follow);
        }else{
            //取消，删除数据
            remove(new QueryWrapper<Follow>()
                    .eq("user_id",userId).eq("follow_user_id",id));
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long id) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", id).count();
        // 3.判断
        return Result.ok(count > 0);
    }
}




