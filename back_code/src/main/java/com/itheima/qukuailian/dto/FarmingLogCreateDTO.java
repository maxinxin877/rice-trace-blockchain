package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 新增农事记录请求（接口文档 v1.1 §5.10）
 */
@Data
public class FarmingLogCreateDTO {

    /** 农事类型：SOWING/FERTILIZATION/PESTICIDE/IRRIGATION/WEEDING/HARVEST */
    @NotBlank(message = "农事类型不能为空")
    private String operationType;

    @NotNull(message = "操作时间不能为空")
    private LocalDateTime operationTime;

    @NotBlank(message = "操作人ID不能为空")
    private String operatorId;

    private String materialName;

    private String materialBatchNo;

    @DecimalMin(value = "0.01", message = "使用量必须大于 0")
    private BigDecimal materialDosage;

    private String materialUnit;

    private Integer safeIntervalDays;

    private String description;

    /** 现场照片、投入品采购凭证文件 ID */
    private List<String> proofFileIds;
}
