package com.dabing.planabc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.entity.Blog;

/**
* @author 22616
* @description 针对表【tb_blog】的数据库操作Service
* @createDate 2022-12-16 13:17:23
*/
public interface BlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
