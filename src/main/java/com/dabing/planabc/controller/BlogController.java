package com.dabing.planabc.controller;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.Blog;
import com.dabing.planabc.service.BlogService;
import com.dabing.planabc.utils.SystemConstants;
import com.dabing.planabc.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {
    @Resource
    private BlogService blogService;

    /**
     * 发布笔记
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current",defaultValue = "1") Integer current){
        return blogService.queryHotBlog(current);
    }

    /**
     * 根据id查询文章信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id")Long id){
        return blogService.queryBlogById(id);
    }

    /**
     * 点赞博客
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id){
        return blogService.likeBlog(id);
    }

    /**
     * 查询博客的点赞情况
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return blogService.queryBlogLikes(id);
    }

    @GetMapping("/of/me")
    public Result queryBlogs(){
        UserDTO userDTO = UserHolder.getUser();
        Long userId = userDTO.getId();
        List<Blog> blogs = blogService.query().eq("user_id", userId).list();
        return Result.ok(blogs);
    }

    /**
     *
     * 查询用户详情页时
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
}
