package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量生成防伪码请求（接口文档 v1.1 §5.24）
 */
@Data
public class TraceCodeGenerateDTO {

    @NotBlank(message = "成品米批次ID不能为空")
    private String productBatchId;

    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为 1")
    @Max(value = 10000, message = "单次生成数量不能超过 10000")
    private Integer quantity;

    @NotBlank(message = "包装规格不能为空")
    private String packageSpec;

    private String expectedSaleRegion;

    /** 二维码有效期天数，默认 0 不限 */
    private Integer expireDays;
}
