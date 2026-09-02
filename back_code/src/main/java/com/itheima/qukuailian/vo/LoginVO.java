package com.itheima.qukuailian.vo;

import lombok.Data;

/**
 * 登录结果（接口文档 v1.1 小程序登录响应风格，管理端沿用）
 */
@Data
public class LoginVO {

    /** JWT 访问令牌 */
    private String token;
    /** 有效期（秒） */
    private Long expiresIn;
    private UserVO user;
}
