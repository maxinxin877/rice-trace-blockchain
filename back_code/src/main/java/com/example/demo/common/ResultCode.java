package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务响应码枚举
 *
 * code   - 业务码（响应体中的 code 字段）
 * http   - HTTP 状态码（响应头状态）
 * msg    - 默认提示信息
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS               (0,     200, "成功"),
    BAD_REQUEST           (40001, 400, "请求参数错误"),
    UNAUTHORIZED          (40101, 401, "未登录或令牌失效"),
    FORBIDDEN             (40301, 403, "无操作权限"),
    NOT_FOUND             (40401, 404, "业务数据不存在"),
    CONFLICT              (40901, 409, "数据状态冲突"),
    VALIDATION_FAILED     (42201, 422, "业务校验失败"),
    INTERNAL_ERROR        (50001, 500, "系统内部异常"),
    BLOCKCHAIN_TX_FAILED  (51001, 500, "区块链交易提交失败"),
    BLOCKCHAIN_VERIFY_FAILED(51002, 500, "链上数据核验失败");

    private final int code;
    private final int httpStatus;
    private final String defaultMessage;
}
