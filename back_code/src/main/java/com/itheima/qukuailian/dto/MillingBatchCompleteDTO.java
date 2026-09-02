package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 完成加工请求（接口文档 v1.1 §5.21）
 */
@Data
public class MillingBatchCompleteDTO {

    @NotNull(message = "精米产出量不能为空")
    @DecimalMin(value = "0.01", message = "精米产出量必须大于 0")
    private BigDecimal riceOutputWeightKg;

    @NotNull(message = "加工结束时间不能为空")
    private LocalDateTime processEndTime;

    /** 最终工艺参数 */
    @NotNull(message = "工艺参数不能为空")
    private Map<String, Object> processParams;

    private String qualitySummary;

    private String qualityReportFileId;

    /** 完成加工说明或审计原因 */
    private String reason;
}
