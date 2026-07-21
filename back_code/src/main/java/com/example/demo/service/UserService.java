package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.dto.RegisterRequest;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据用户名查找用户（含角色）
     */
    User findByUsername(String username);

    /**
     * 根据ID查找用户
     */
    User findById(Long id);

    /**
     * 用户注册
     */
    User register(RegisterRequest request);

    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);
}
