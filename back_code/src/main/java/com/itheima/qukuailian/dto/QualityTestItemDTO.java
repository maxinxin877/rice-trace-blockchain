package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 质检项目（接口文档 v1.1 §5.17 testItems[]）
 */
@Data
public class QualityTestItemDTO {

    @NotBlank(message = "检测项目不能为空")
    private String itemName;

    @NotNull(message = "检测值不能为空")
    @DecimalMin(value = "0", message = "检测值不能为负数")
    private BigDecimal value;

    private String unit;

    private String standardValue;

    @NotBlank(message = "单项结果不能为空")
    private String result;
}
