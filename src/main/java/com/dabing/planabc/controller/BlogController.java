package com.dabing.planabc.controller;

import com.dabing.planabc.dto.Result;
import com.dabing.planabc.service.BlogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/blog")
public class BlogController {
    @Resource
    private BlogService blogService;

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
        return Result.ok(blogService.getById(id));
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
}
