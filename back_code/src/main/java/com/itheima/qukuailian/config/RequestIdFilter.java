package com.itheima.qukuailian.config;

import com.itheima.qukuailian.common.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 请求追踪 ID 过滤器：从 X-Request-Id 请求头读取，缺失则自动生成，写入 RequestContext。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestId = request.getHeader("X-Request-Id");
            if (!StringUtils.hasText(requestId)) {
                requestId = "req-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                        + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            }
            RequestContext.setRequestId(requestId);
            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }
}
