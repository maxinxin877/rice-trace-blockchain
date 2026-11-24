package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建召回任务（需求文档 §六-4：质量检测不合格、投诉举报、监管指令均可触发）
 */
@Data
public class RecallCreateDTO {

    /** 触发类型：QUALITY_FAIL/CHANNEL_RISK/COMPLAINT/REGULATOR/ENTERPRISE */
    @NotBlank(message = "触发类型不能为空")
    @Pattern(regexp = "QUALITY_FAIL|CHANNEL_RISK|COMPLAINT|REGULATOR|ENTERPRISE",
            message = "触发类型只能是 QUALITY_FAIL/CHANNEL_RISK/COMPLAINT/REGULATOR/ENTERPRISE")
    private String triggerType;

    /** 触发业务类型：QUALITY_TEST/TRACE_CODE/PRODUCT_BATCH/GRAIN_BATCH/PLANTING_BATCH */
    private String triggerBusinessType;

    /** 触发业务 ID（不传则用 scopeBusinessId） */
    private String triggerBusinessId;

    /** 影响范围计算的起点类型，默认与 triggerBusinessType 一致 */
    private String scopeBusinessType;

    /** 影响范围计算的起点 ID，默认与 triggerBusinessId 一致 */
    private String scopeBusinessId;

    /** 风险等级：LOW/MEDIUM/HIGH */
    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "风险等级只能是 LOW/MEDIUM/HIGH")
    private String riskLevel;

    @NotBlank(message = "召回原因不能为空")
    @Size(max = 500, message = "召回原因不超过 500 字")
    private String reason;

    /** 是否把受影响的单个防伪码也纳入明细（默认 true，便于终端逐码冻结） */
    private Boolean includeTraceCodes;

    /** 明细最多纳入的防伪码条数（默认 200，避免一次生成上万条） */
    private Integer maxTraceCodeItems;

    /** 处置截止时间 */
    private LocalDateTime deadline;
}
