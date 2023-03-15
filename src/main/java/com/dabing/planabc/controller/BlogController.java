package com.dabing.planabc.controller;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.dabing.planabc.dto.Result;
import com.dabing.planabc.dto.UserDTO;
import com.dabing.planabc.entity.Blog;
import com.dabing.planabc.service.BlogService;
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
        Long userId = UserHolder.getUser().getId();
        blog.setUserId(userId);
        boolean isSuccess = blogService.save(blog);
        if(isSuccess)
            return Result.ok();
        else
            return Result.fail("发布笔记失败");
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


    @GetMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id){
        blogService.update()
                .setSql("liked=liked+1").eq("id",id).update();
        return Result.ok();
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return Result.ok();
    }

    @GetMapping("/of/me")
    public Result queryBlogs(){
        UserDTO userDTO = UserHolder.getUser();
        Long userId = userDTO.getId();
        List<Blog> blogs = blogService.query().eq("user_id", userId).list();
        return Result.ok(blogs);
    }
}
