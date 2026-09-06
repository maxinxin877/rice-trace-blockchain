package com.example.demo.controller;

import com.example.demo.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 测试 Controller
 * 演示不同角色的权限控制
 */
@RestController
@RequestMapping("/api")
public class TestController {

    /**
     * 公开接口（无需认证）
     */
    @GetMapping("/public/hello")
    public Result<String> publicHello() {
        return Result.success("Hello, this is a public endpoint!");
    }

    /**
     * 需要认证的接口（任何登录用户可访问）
     */
    @GetMapping("/user/info")
    public Result<Map<String, String>> userInfo() {
        return Result.success(Map.of("message", "This is a user-level endpoint"));
    }

    /**
     * 需要 ADMIN 角色的接口
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> adminDashboard() {
        return Result.success(Map.of(
                "message", "Welcome to admin dashboard!",
                "level", "admin"
        ));
    }

    /**
     * 需要 ADMIN 或 MANAGER 角色的接口
     */
    @GetMapping("/manager/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Result<Map<String, String>> managerReports() {
        return Result.success(Map.of(
                "message", "Reports data",
                "level", "manager"
        ));
    }
}
