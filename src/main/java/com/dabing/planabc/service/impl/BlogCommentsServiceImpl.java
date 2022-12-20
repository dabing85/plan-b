package com.dabing.planabc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabing.planabc.entity.BlogComments;
import com.dabing.planabc.mapper.BlogCommentsMapper;
import com.dabing.planabc.service.BlogCommentsService;
import org.springframework.stereotype.Service;

/**
* @author 22616
* @description 针对表【tb_blog_comments】的数据库操作Service实现
* @createDate 2022-12-16 13:19:16
*/
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments>
    implements BlogCommentsService {

}




