package com.dabing.planabc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.Follow;
import com.dabing.planabc.mapper.FollowMapper;
import com.dabing.planabc.service.FollowService;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 22616
* @description 针对表【tb_follow】的数据库操作Service实现
* @createDate 2022-12-16 13:19:22
*/
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
    implements FollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService;

    @Override
    public Result follow(Long id, Boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return Result.fail("未登录");
        }
        Long userId = user.getId();
        String key="follows:"+userId;
        //1.判断是关注还是取消
        if(BooleanUtil.isTrue(isFollow)){
            //关注，添加关注数据
            Follow follow=new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            save(follow);
            //将用户添加到set集合
            stringRedisTemplate.opsForSet().add(key,id.toString());
        }else{
            //取消，删除数据
            remove(new QueryWrapper<Follow>()
                    .eq("user_id",userId).eq("follow_user_id",id));
            //将该用户从set中移除
            stringRedisTemplate.opsForSet().remove(key,id.toString());
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

    @Override
    public Result commonFollow(Long id) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 2.求交集
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 无交集
            return Result.ok(Collections.emptyList());
        }
        // 3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4.查询用户
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}




