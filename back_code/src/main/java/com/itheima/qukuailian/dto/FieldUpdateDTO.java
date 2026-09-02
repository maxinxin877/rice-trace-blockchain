package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 更新地块请求（接口文档 v1.1 §5.4，完整覆盖 + 审计原因）
 */
@Data
public class FieldUpdateDTO {

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

    @NotNull(message = "GIS边界坐标不能为空")
    @Size(min = 3, message = "GIS边界坐标至少包含 3 个坐标点")
    private List<GisPoint> gisBoundary;

    private String soilType;

    private List<String> basePhotoFileIds;

    /** 修改原因（必填，写入审计日志） */
    @NotBlank(message = "修改原因不能为空")
    private String reason;
}
