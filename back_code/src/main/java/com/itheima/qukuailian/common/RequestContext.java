package com.itheima.qukuailian.common;

/**
 * 请求上下文（基于 ThreadLocal）
 * <p>由 {@code RequestIdFilter} 在每个请求进入时写入 requestId，响应组装时读取。</p>
 */
public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID_HOLDER.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID_HOLDER.get();
    }

    public static void clear() {
        REQUEST_ID_HOLDER.remove();
    }
}
