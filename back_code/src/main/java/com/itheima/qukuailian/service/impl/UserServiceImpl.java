package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.LoginDTO;
import com.itheima.qukuailian.dto.RegisterDTO;
import com.itheima.qukuailian.entity.User;
import com.itheima.qukuailian.mapper.UserMapper;
import com.itheima.qukuailian.service.UserService;
import com.itheima.qukuailian.utils.JwtUtils;
import com.itheima.qukuailian.utils.PermissionConstants;
import com.itheima.qukuailian.utils.UserContext;
import com.itheima.qukuailian.vo.LoginVO;
import com.itheima.qukuailian.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtils jwtUtils;

    @Override
    public void register(RegisterDTO dto) {
        long count = lambdaQuery().eq(User::getUsername, dto.getUsername()).count();
        if (count > 0) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        // 演示项目使用 MD5(盐 + 密码)，生产环境请替换为 BCrypt 等加盐哈希算法
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        user.setSalt(salt);
        user.setPassword(encrypt(dto.getPassword(), salt));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole() == null ? "FARMER" : dto.getRole());
        user.setStatus(1);
        save(user);
        log.info("用户注册成功: {}", dto.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = lambdaQuery().eq(User::getUsername, dto.getUsername()).one();
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户名或密码错误");
        }
        if (!encrypt(dto.getPassword(), user.getSalt()).equals(user.getPassword())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
        // JWT：携带用户基本信息与角色权限
        String token = jwtUtils.createToken(user.getId(), user.getUsername(), user.getRole(),
                PermissionConstants.permissionsOf(user.getRole()));

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtUtils.getExpireSeconds());
        vo.setUser(toUserVO(user));
        return vo;
    }

    @Override
    public void logout(String token) {
        // JWT 无状态，客户端丢弃令牌即可；如需要可在此接入黑名单（Redis）
        log.info("用户退出登录");
    }

    @Override
    public UserVO getLoginUser() {
        User user = getById(UserContext.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public IPage<User> pageUsers(long pageNo, long pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    private String encrypt(String rawPassword, String salt) {
        return DigestUtils.md5DigestAsHex((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
