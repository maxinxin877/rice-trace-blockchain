package com.itheima.qukuailian.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用错误码（接口文档 v1.1 通用错误码）
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),
    /** 400：请求参数错误 */
    PARAM_ERROR(40001, "请求参数错误"),
    /** 401：未登录或令牌失效 */
    UNAUTHORIZED(40101, "未登录或令牌失效"),
    /** 403：无操作权限 */
    FORBIDDEN(40301, "无操作权限"),
    /** 404：业务数据不存在 */
    NOT_FOUND(404701, "业务数据不存在"),
    /** 409：数据状态冲突（如防伪码已激活） */
    CONFLICT(40901, "数据状态冲突"),
    /** 422：业务校验失败（如产量平衡异常） */
    VALIDATION_FAILED(42201, "业务校验失败"),
    /** 500：系统内部异常 */
    ERROR(50001, "系统内部异常"),
    /** 500：区块链交易提交失败 */
    CHAIN_SUBMIT_FAILED(51001, "区块链交易提交失败"),
    /** 500：链上数据核验失败 */
    CHAIN_VERIFY_FAILED(51002, "链上数据核验失败");

    private final Integer code;
    private final String message;
}
