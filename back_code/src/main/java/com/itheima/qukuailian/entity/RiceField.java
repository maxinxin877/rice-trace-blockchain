package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.itheima.qukuailian.dto.GisPoint;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 地块档案（接口文档 v1.1 §3.1 RiceField）
 */
@Data
@TableName(value = "rice_field", autoResultMap = true)
public class RiceField {

    @TableId(type = IdType.INPUT)
    private String fieldId;

    /** 地块编号（租户内唯一） */
    private String fieldCode;

    private String fieldName;

    /** 种植户 ID */
    private String farmerId;

    private String farmerName;

    private String province;
    private String city;
    private String district;
    private String address;

    /** 面积（亩） */
    private BigDecimal areaMu;

    /** GIS 边界坐标（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<GisPoint> gisBoundary;

    private String soilType;

    /** 基地实景照片文件 ID 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> basePhotoFileIds;

    /** 地块坐标 SHA-256 哈希 */
    private String coordinateHash;

    /** 上链状态：PENDING/SUCCESS/FAILED */
    private String chainStatus;

    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 绑定的在种批次摘要（非表字段，详情接口填充） */
    @TableField(exist = false)
    private List<RicePlantingBatch> plantingBatches;
}
