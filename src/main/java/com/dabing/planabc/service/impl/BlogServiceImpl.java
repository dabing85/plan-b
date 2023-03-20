package com.dabing.planabc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.Blog;
import com.dabing.planabc.entity.Follow;
import com.dabing.planabc.entity.User;
import com.dabing.planabc.mapper.BlogMapper;
import com.dabing.planabc.service.BlogService;
import com.dabing.planabc.service.FollowService;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.SystemConstants;
import com.dabing.planabc.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dabing.planabc.utils.RedisConstants.BLOG_LIKE_KEY;

/**
* @author 22616
* @description 针对表【tb_blog】的数据库操作Service实现
* @createDate 2022-12-16 13:17:23
*/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService {
    @Resource
    private UserService userService;
    @Resource
    private FollowService followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询当前页的文章,按点赞数排序
     * @param current
     * @return
     */
    @Override
    public Result queryHotBlog(Integer current) {
        //根据点赞数查询当前页数据
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        //获取当前页数据
        List<Blog> records = page.getRecords();
        //根据blog查询用户信息
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * 通过id查询笔记详情
     */
    @Override
    public Result queryBlogById(Long id) {
        //查询笔记信息
        Blog blog = getById(id);
        //查询笔记的用户信息
        queryBlogUser(blog);
        //查询登录用户是都已经点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        //1.获取登录用户信息
        Long userId = UserHolder.getUser().getId();
        //2.判断当前用户是否已经点赞
        String key=BLOG_LIKE_KEY+id;
        //Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        //使用zset类型，实现用户点赞排行,根据时间排行
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        if(score==null){
            //未点赞，+1 update tb_blog set liked=liked+1 where id=?
            boolean isSuccess = update().setSql("liked = liked+1").eq("id", id).update();
            if(isSuccess){
                //将用户id添加到set集合中
//                stringRedisTemplate.opsForSet().add(key,userId.toString());
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {
            //已点赞，取消点赞
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(isSuccess){
                //将用户id从set集合中移除
//                stringRedisTemplate.opsForSet().remove(key,userId.toString());
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        //从zset中读取当前博客的点赞top5列表
        String key = BLOG_LIKE_KEY+id;
        Set<String> userSet = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(userSet==null || userSet.isEmpty()){
            //没有人点赞
            return Result.ok(Collections.emptyList());
        }
        //解析数据
        List<Long> userIdList = userSet.stream().map(Long::valueOf).collect(Collectors.toList());
        String strSql= StrUtil.join(",",userIdList);
        //select * from user where id in (1,2,3,4,5) order by field (1,2,3,4,5);
        List<User> users = userService.query().in("id", userIdList).last("order by field (id," + strSql + ")").list();
        //读取数据中的用户信息并返回
        List<UserDTO> userDTOS = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    /**
     * 保存笔记的时候使用feed流推模式把笔记保存到粉丝的收件箱中，使用zset结构保存数据
     */
    @Override
    public Result saveBlog(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        blog.setUserId(userId);
        boolean isSuccess = save(blog);
        if(!isSuccess)
            return Result.fail("发布笔记失败");
        //查询粉丝
        List<Follow> fans = followService.query().eq("follow_user_id", userId).list();
        for(Follow fan:fans){
            //保存博客到粉丝的收件箱
            String key="feed:"+fan.getUserId();
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
        return Result.ok(blog.getId());
    }

    /**
     * 判断用户是否点赞过该博客
     */
    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKE_KEY + blog.getId();
//        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}




