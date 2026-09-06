package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建入库单请求（接口文档 v1.1 §5.14）
 */
@Data
public class StorageReceiptCreateDTO {

    @NotBlank(message = "种植批次ID不能为空")
    private String plantingBatchId;

    @NotBlank(message = "原粮批次ID不能为空")
    private String grainBatchId;

    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    @NotBlank(message = "入仓编号不能为空")
    private String warehouseCode;

    @NotNull(message = "收割面积不能为空")
    @DecimalMin(value = "0.01", message = "收割面积必须大于 0")
    private BigDecimal harvestAreaMu;

    @NotNull(message = "湿谷重量不能为空")
    @DecimalMin(value = "0.01", message = "湿谷重量必须大于 0")
    private BigDecimal wetGrainWeightKg;

    @NotNull(message = "水分含量不能为空")
    private BigDecimal moisturePercent;

    @NotNull(message = "杂质含量不能为空")
    private BigDecimal impurityPercent;

    @NotBlank(message = "原粮等级不能为空")
    private String grainGrade;

    @NotNull(message = "入库时间不能为空")
    private LocalDateTime storageTime;

    private BigDecimal temperature;
    private BigDecimal humidity;
    private String keeperSignature;
    private String farmerSignature;
}
