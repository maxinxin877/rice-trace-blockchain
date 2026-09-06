package com.itheima.qukuailian.config;

import com.itheima.qukuailian.common.LoginUser;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.utils.JwtUtils;
import com.itheima.qukuailian.utils.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 认证 + 权限拦截器：
 * <ol>
 *   <li>校验 {@code Authorization: Bearer {JWT}}，解析后写入 UserContext</li>
 *   <li>校验方法/类上的 {@code @RequirePermission} 权限码</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        String token = authorization.substring(BEARER_PREFIX.length());

        final Claims claims;
        try {
            claims = jwtUtils.parseToken(token);
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        @SuppressWarnings("unchecked")
        Set<String> permissions = new HashSet<>((List<String>) claims.get("permissions", List.class));

        UserContext.setLoginUser(new LoginUser(userId, username, role, permissions));

        // 权限校验
        if (handler instanceof HandlerMethod handlerMethod) {
            RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
            if (requirePermission == null) {
                requirePermission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
            }
            if (requirePermission != null && !permissions.contains(requirePermission.value())) {
                throw new BizException(ResultCode.FORBIDDEN);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
