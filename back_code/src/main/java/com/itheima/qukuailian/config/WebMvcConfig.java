package com.itheima.qukuailian.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：跨域 + 拦截器注册
 * <p>基础路径已通过 server.servlet.context-path=/api/v1 统一处理。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // /auth/login 和 /auth/register 放行，其他 /auth/** 需要认证
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/rice/**", "/files/**", "/auth/**")
                .excludePathPatterns("/auth/login", "/auth/register");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 开发阶段放开跨域，生产环境由 Nginx 反向代理同源访问，可删除
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
