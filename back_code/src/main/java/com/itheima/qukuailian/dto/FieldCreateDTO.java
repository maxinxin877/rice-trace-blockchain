package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建地块请求（接口文档 v1.1 §5.1）
 */
@Data
public class FieldCreateDTO {

    @NotBlank(message = "地块编号不能为空")
    private String fieldCode;

    @NotBlank(message = "地块名称不能为空")
    private String fieldName;

    @NotBlank(message = "种植户ID不能为空")
    private String farmerId;

    @NotBlank(message = "省不能为空")
    private String province;

    @NotBlank(message = "市不能为空")
    private String city;

    @NotBlank(message = "区县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String address;

    @NotNull(message = "面积不能为空")
    @DecimalMin(value = "0.01", message = "面积必须大于 0")
    private BigDecimal areaMu;

    /** GIS 边界坐标，至少 3 个点 */
    @NotNull(message = "GIS边界坐标不能为空")
    @Size(min = 3, message = "GIS边界坐标至少包含 3 个坐标点")
    private List<GisPoint> gisBoundary;

    private String soilType;

    private List<String> basePhotoFileIds;
}
