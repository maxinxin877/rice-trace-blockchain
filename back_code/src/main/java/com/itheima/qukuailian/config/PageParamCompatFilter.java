package com.itheima.qukuailian.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 分页参数兼容过滤器。
 * <p>接口文档 §2.3 规定分页参数为 {@code pageNo}，部分前端习惯传 {@code page}；
 * 本过滤器在 {@code pageNo} 缺失时用 {@code page} 的值补齐，
 * 使两种写法都能正确翻页，无需逐个接口做兼容。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PageParamCompatFilter extends OncePerRequestFilter {

    private static final String DOC_PAGE_PARAM = "pageNo";
    private static final String LEGACY_PAGE_PARAM = "page";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String pageNo = request.getParameter(DOC_PAGE_PARAM);
        String page = request.getParameter(LEGACY_PAGE_PARAM);
        if (!StringUtils.hasText(pageNo) && StringUtils.hasText(page)) {
            filterChain.doFilter(new PageAliasRequest(request, page), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** 将 page 参数以 pageNo 名称暴露给后续参数绑定 */
    private static class PageAliasRequest extends HttpServletRequestWrapper {

        private final String pageValue;

        PageAliasRequest(HttpServletRequest request, String pageValue) {
            super(request);
            this.pageValue = pageValue;
        }

        @Override
        public String getParameter(String name) {
            if (DOC_PAGE_PARAM.equals(name)) {
                return pageValue;
            }
            return super.getParameter(name);
        }

        @Override
        public String[] getParameterValues(String name) {
            if (DOC_PAGE_PARAM.equals(name)) {
                return new String[]{pageValue};
            }
            return super.getParameterValues(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = new HashMap<>(super.getParameterMap());
            map.put(DOC_PAGE_PARAM, new String[]{pageValue});
            return Collections.unmodifiableMap(map);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            Map<String, String[]> map = getParameterMap();
            return Collections.enumeration(map.keySet());
        }
    }
}
