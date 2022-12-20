package com.dabing.planabc.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Blog;
import com.dabing.planabc.entity.User;
import com.dabing.planabc.mapper.BlogMapper;
import com.dabing.planabc.service.BlogService;
import com.dabing.planabc.service.UserService;
import com.dabing.planabc.utils.SystemConstants;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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

    private void isBlogLiked(Blog blog) {
        //1.获取登录用户信息

        //2.判断当前登录用户是否已经点赞
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}




