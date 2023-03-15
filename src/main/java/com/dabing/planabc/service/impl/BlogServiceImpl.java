package com.dabing.planabc.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Blog;
import com.dabing.planabc.entity.User;
import com.dabing.planabc.mapper.BlogMapper;
import com.dabing.planabc.service.BlogService;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.SystemConstants;
import com.dabing.planabc.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        
        if(BooleanUtil.isFalse(isMember)){
            //未点赞，+1 update tb_blog set liked=liked+1 where id=?
            boolean isSuccess = update().setSql("liked = liked+1").eq("id", id).update();
            if(isSuccess){
                //将用户id添加到set集合中
                stringRedisTemplate.opsForSet().add(key,userId.toString());
            }
        }else {
            //已点赞，取消点赞
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(isSuccess){
                //将用户id从set集合中移除
                stringRedisTemplate.opsForSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 判断用户是否点赞过该博客
     */
    private void isBlogLiked(Blog blog) {
        //1.获取登录用户信息
        Long userId = UserHolder.getUser().getId();
        Long id = blog.getId();
        //2.判断当前登录用户是否已经点赞
        String key=BLOG_LIKE_KEY+id;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        blog.setIsLike(isMember);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}




