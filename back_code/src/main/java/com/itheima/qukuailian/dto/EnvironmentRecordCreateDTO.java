package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传稻田环境数据请求（接口文档 v1.1 §5.12）
 */
@Data
public class EnvironmentRecordCreateDTO {

    /** 数据来源：IOT/DRONE/MANUAL */
    @NotBlank(message = "数据来源不能为空")
    private String sourceType;

    @NotNull(message = "采集时间不能为空")
    private LocalDateTime recordTime;

    private BigDecimal airTemperature;
    private BigDecimal airHumidity;
    private BigDecimal soilMoisture;
    private BigDecimal rainfall;
    private BigDecimal windSpeed;

    private List<String> imageFileIds;

    private String remark;
}
