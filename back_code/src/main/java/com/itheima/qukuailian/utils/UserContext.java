package com.itheima.qukuailian.utils;

import com.itheima.qukuailian.common.LoginUser;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;

/**
 * 当前登录用户上下文（基于 ThreadLocal，由 AuthInterceptor 写入）
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setLoginUser(LoginUser user) {
        USER_HOLDER.set(user);
    }

    public static LoginUser getLoginUser() {
        LoginUser user = USER_HOLDER.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
