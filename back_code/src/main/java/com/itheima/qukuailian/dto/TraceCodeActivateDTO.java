package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 激活防伪码请求（接口文档 v1.1 §5.25）
 */
@Data
public class TraceCodeActivateDTO {

    @NotBlank(message = "成品米批次ID不能为空")
    private String productBatchId;

    @NotEmpty(message = "防伪码列表不能为空")
    private List<String> traceCodes;

    @NotBlank(message = "激活人ID不能为空")
    private String activatedBy;

    /** 不传则取服务器时间 */
    private LocalDateTime activatedAt;

    /** 激活说明或审计原因 */
    private String reason;
}
