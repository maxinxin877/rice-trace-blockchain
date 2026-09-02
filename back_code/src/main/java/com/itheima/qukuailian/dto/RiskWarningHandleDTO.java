package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 处理风险预警请求（接口文档 v1.1 §7.4）
 */
@Data
public class RiskWarningHandleDTO {

    /** 处理结果 */
    @NotBlank(message = "处理结果不能为空")
    private String handleResult;

    /** 处理原因，写入审计日志 */
    @NotBlank(message = "处理原因不能为空")
    private String reason;
}
