package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 稻田环境数据（接口文档 v1.1 §3.7 RiceEnvironmentRecord）
 */
@Data
@TableName(value = "rice_environment_record", autoResultMap = true)
public class RiceEnvironmentRecord {

    @TableId(type = IdType.INPUT)
    private String environmentRecordId;

    private String plantingBatchId;

    /** 数据来源：IOT/DRONE/MANUAL */
    private String sourceType;

    private LocalDateTime recordTime;

    /** 空气温度(℃) */
    private BigDecimal airTemperature;

    /** 空气湿度(%) */
    private BigDecimal airHumidity;

    /** 土壤湿度(%) */
    private BigDecimal soilMoisture;

    /** 降雨量(mm) */
    private BigDecimal rainfall;

    /** 风速(m/s) */
    private BigDecimal windSpeed;

    /** 无人机/现场图片文件 ID 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imageFileIds;

    private String remark;

    private LocalDateTime createTime;
}
