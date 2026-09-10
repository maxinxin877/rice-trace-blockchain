package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.ResultCode;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证 Controller
 * 处理登录、注册等不需要认证的接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String username = authentication.getName();

        // 获取用户角色
        User user = userService.findByUsername(username);
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // 生成 Token
        String token = jwtTokenProvider.generateToken(username, roles);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(username)
                .roles(roles)
                .build();

        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        if (userService.existsByUsername(request.getUsername())) {
            return Result.error(ResultCode.CONFLICT, "用户名已存在");
        }

        // 注册用户
        User user = userService.register(request);

        // 生成 Token
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        String token = jwtTokenProvider.generateToken(user.getUsername(), roles);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .roles(roles)
                .build();

        return Result.success("注册成功", response);
    }

    /**
     * 获取当前用户信息（需要认证）
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        // 隐藏密码
        user.setPassword(null);
        return Result.success(user);
    }
}
