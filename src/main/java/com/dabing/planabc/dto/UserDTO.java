package com.dabing.planabc.dto;

import lombok.Data;

/**
 * 脱敏User信息
 */
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
