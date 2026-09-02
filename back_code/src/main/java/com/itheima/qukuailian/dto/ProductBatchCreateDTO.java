package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建成品米批次请求（接口文档 v1.1 §5.22，productBatchId 由客户端传入且业务唯一）
 */
@Data
public class ProductBatchCreateDTO {

    @NotBlank(message = "成品米批次ID不能为空")
    private String productBatchId;

    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @NotBlank(message = "水稻品种不能为空")
    private String riceVariety;

    @NotBlank(message = "品牌名称不能为空")
    private String brandName;

    @NotBlank(message = "包装规格不能为空")
    private String packageSpec;

    @NotBlank(message = "执行标准号不能为空")
    private String standardNo;

    private Map<String, Object> nutritionFacts;

    private List<String> certificationFileIds;

    private String expectedSaleRegion;
}
