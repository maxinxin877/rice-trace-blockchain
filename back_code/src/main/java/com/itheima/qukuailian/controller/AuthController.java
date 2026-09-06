package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.dto.LoginDTO;
import com.itheima.qukuailian.dto.RegisterDTO;
import com.itheima.qukuailian.service.UserService;
import com.itheima.qukuailian.vo.LoginVO;
import com.itheima.qukuailian.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公共认证模块（基础路径 /api/v1 由 context-path 统一处理）
 * <ul>
 *   <li>POST /api/v1/auth/login —— 登录，返回 Bearer JWT</li>
 *   <li>POST /api/v1/auth/register —— 注册</li>
 *   <li>GET  /api/v1/auth/me —— 当前登录用户</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("success", userService.login(dto));
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getLoginUser());
    }

    /** 退出登录（JWT 无状态，客户端丢弃令牌即可） */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        userService.logout(authorization);
        return Result.success();
    }
}
