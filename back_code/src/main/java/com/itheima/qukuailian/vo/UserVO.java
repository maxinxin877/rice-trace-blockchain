package com.itheima.qukuailian.vo;

import lombok.Data;

/**
 * 用户信息（脱敏，不含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String role;
    private Integer status;
}
