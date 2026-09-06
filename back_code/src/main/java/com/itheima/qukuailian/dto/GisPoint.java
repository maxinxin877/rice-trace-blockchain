package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * GIS 坐标点
 */
@Data
public class GisPoint {

    @NotNull(message = "经度不能为空")
    private Double lng;

    @NotNull(message = "纬度不能为空")
    private Double lat;
}
