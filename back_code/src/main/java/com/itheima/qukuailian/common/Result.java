package com.itheima.qukuailian.common;

import lombok.Data;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应结果（接口文档 v1.1 通用响应规范）
 * <pre>
 * {
 *   "code": 0, "message": "success", "data": {},
 *   "requestId": "req-20260718-000001",
 *   "timestamp": "2026-07-18T10:20:30+08:00"
 * }
 * </pre>
 */
@Data
public class Result<T> {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 0 成功；其它为错误码（见 ResultCode） */
    private Integer code;
    private String message;
    private T data;
    /** 客户端请求追踪 ID（取 X-Request-Id 或自动生成） */
    private String requestId;
    /** 服务器时间（东八区） */
    private String timestamp;

    public static <T> Result<T> success() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return build(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail(String message) {
        return build(ResultCode.ERROR.getCode(), message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return build(code, message, null);
    }

    private static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setRequestId(RequestContext.getRequestId());
        result.setTimestamp(OffsetDateTime.now(ZoneOffset.ofHours(8)).format(TIME_FORMATTER));
        return result;
    }
}
