package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 水稻种植批次（接口文档 v1.1 §3.2 RicePlantingBatch）
 */
@Data
@TableName(value = "rice_planting_batch", autoResultMap = true)
public class RicePlantingBatch {

    @TableId(type = IdType.INPUT)
    private String plantingBatchId;

    private String fieldId;

    /** 水稻品种 */
    private String riceVariety;

    /** 种子来源 */
    private String seedSource;

    private String seedBatchNo;

    /** 种子证明文件 ID */
    private String seedCertificateFileId;

    private LocalDate sowingDate;
    private LocalDate expectedHarvestDate;
    private LocalDate actualHarvestDate;

    /** 状态：DRAFT/PLANTED/HARVESTED/STORED/MILLING/PACKAGED/ON_SALE/LOCKED */
    private String status;

    private Boolean organicCertified;
    private Boolean greenCertified;

    /** 认证证书文件 ID 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> certificationFileIds;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 地块摘要（非表字段，详情接口填充） */
    @TableField(exist = false)
    private RiceField field;

    /** 农事记录数（非表字段，详情接口填充） */
    @TableField(exist = false)
    private Integer farmingLogCount;

    /** 环境数据条数（非表字段，详情接口填充） */
    @TableField(exist = false)
    private Integer environmentRecordCount;
}
