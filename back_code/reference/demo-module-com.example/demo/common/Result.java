package com.example.demo.common;

import lombok.Data;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一API响应结果封装
 */
@Data
public class Result<T> {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static volatile String currentDatePrefix;

    private int code;
    private String message;
    private T data;
    private String requestId;
    private String timestamp;

    private Result() {
        this.requestId = generateRequestId();
        this.timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static synchronized String generateRequestId() {
        String today = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(currentDatePrefix)) {
            currentDatePrefix = today;
            SEQ.set(0);
        }
        int seq = SEQ.incrementAndGet();
        return String.format("req-%s-%06d", today, seq);
    }

    // ========== 成功 ==========

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getDefaultMessage();
        r.data = data;
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = message;
        r.data = data;
        return r;
    }

    // ========== 失败 ==========

    /**
     * 使用 ResultCode 默认提示信息
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.code = resultCode.getCode();
        r.message = resultCode.getDefaultMessage();
        return r;
    }

    /**
     * 使用 ResultCode + 自定义提示信息
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Result<T> r = new Result<>();
        r.code = resultCode.getCode();
        r.message = message;
        return r;
    }
}
