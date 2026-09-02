package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 创建加工批次请求（接口文档 v1.1 §5.18）
 */
@Data
public class MillingBatchCreateDTO {

    @NotBlank(message = "原粮批次ID不能为空")
    private String grainBatchId;

    @NotBlank(message = "成品米批次ID不能为空")
    private String productBatchId;

    @NotBlank(message = "加工厂ID不能为空")
    private String factoryId;

    @NotNull(message = "稻谷出库量不能为空")
    @DecimalMin(value = "0.01", message = "稻谷出库量必须大于 0")
    private BigDecimal grainOutWeightKg;

    @NotNull(message = "加工开始时间不能为空")
    private LocalDateTime processStartTime;

    /** 脱壳/抛光/色选/包装参数 */
    @NotNull(message = "工艺参数不能为空")
    private Map<String, Object> processParams;
}
