package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 执行产量平衡校验请求（接口文档 v1.1 §7.1）
 */
@Data
public class YieldBalanceCheckDTO {

    private String plantingBatchId;
    private String grainBatchId;
    private String productBatchId;

    /** 校验范围：PLANTING_TO_STORAGE/STORAGE_TO_MILLING/FULL_CHAIN */
    @NotBlank(message = "校验范围不能为空")
    private String checkScope;
}
