package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 角色实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Long id;
    private String name;        // 角色名称，如 ROLE_ADMIN, ROLE_USER
    private String description;
}
