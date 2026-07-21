package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Integer enabled;     // 1-启用，0-禁用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 用户拥有的角色（非数据库字段）
    private Set<Role> roles;
}
