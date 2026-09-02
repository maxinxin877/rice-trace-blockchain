package com.itheima.qukuailian.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 当前登录用户信息（从 JWT 解析，存放在 ThreadLocal）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String role;
    private Set<String> permissions;
}
