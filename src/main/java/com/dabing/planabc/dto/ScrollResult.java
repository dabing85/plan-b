package com.dabing.planabc.dto;

import lombok.Data;

import java.util.List;

/**
 * 查询关注用户的blog信息时使用滚动分页返回结果
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
