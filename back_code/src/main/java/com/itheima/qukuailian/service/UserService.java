package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.qukuailian.dto.LoginDTO;
import com.itheima.qukuailian.dto.RegisterDTO;
import com.itheima.qukuailian.entity.User;
import com.itheima.qukuailian.vo.LoginVO;
import com.itheima.qukuailian.vo.UserVO;

public interface UserService extends IService<User> {

    /** 注册 */
    void register(RegisterDTO dto);

    /** 登录：成功返回 token */
    LoginVO login(LoginDTO dto);

    /** 退出登录 */
    void logout(String token);

    /** 获取当前登录用户 */
    UserVO getLoginUser();

    /** 用户分页（管理端） */
    IPage<User> pageUsers(long page, long size, String keyword);
}
