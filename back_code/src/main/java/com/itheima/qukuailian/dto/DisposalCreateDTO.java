package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 不合格品处置单创建（需求文档 §九 FC-04）
 */
@Data
public class DisposalCreateDTO {

    /** QUALITY_TEST/SCAN_CHECK/INSPECTION/MANUAL/COMPLAINT */
    private String sourceType;

    private String sourceId;

    @NotBlank(message = "问题对象类型不能为空")
    @Pattern(regexp = "GRAIN_BATCH|PRODUCT_BATCH|MILLING_BATCH|TRACE_CODE",
            message = "问题对象类型只能是 GRAIN_BATCH/PRODUCT_BATCH/MILLING_BATCH/TRACE_CODE")
    private String businessType;

    @NotBlank(message = "问题对象ID不能为空")
    private String businessId;

    private String batchNo;

    private String productName;

    @NotBlank(message = "缺陷类型不能为空")
    private String defectType;

    @NotBlank(message = "问题描述不能为空")
    @Size(max = 500, message = "问题描述不超过 500 字")
    private String defectDesc;

    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "风险等级只能是 LOW/MEDIUM/HIGH")
    private String riskLevel;

    private Integer quantity;

    private BigDecimal weightKg;

    /** ISOLATE/REWORK/REPAIR/SCRAP/CONCESSION/RELEASE */
    @NotBlank(message = "处置方式不能为空")
    @Pattern(regexp = "ISOLATE|REWORK|REPAIR|SCRAP|CONCESSION|RELEASE",
            message = "处置方式只能是 ISOLATE/REWORK/REPAIR/SCRAP/CONCESSION/RELEASE")
    private String disposition;
}
