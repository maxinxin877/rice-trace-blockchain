package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收储入库单（接口文档 v1.1 §3.4 RiceStorageReceipt）
 */
@Data
@TableName("rice_storage_receipt")
public class RiceStorageReceipt {

    @TableId(type = IdType.INPUT)
    private String storageReceiptId;

    private String plantingBatchId;

    /** 原粮批次 ID（业务唯一） */
    private String grainBatchId;

    private String warehouseId;

    /** 入仓编号 */
    private String warehouseCode;

    /** 收割面积（亩） */
    private BigDecimal harvestAreaMu;

    /** 湿谷重量（kg） */
    private BigDecimal wetGrainWeightKg;

    /** 水分含量（%） */
    private BigDecimal moisturePercent;

    /** 杂质含量（%） */
    private BigDecimal impurityPercent;

    /** 原粮等级 */
    private String grainGrade;

    private LocalDateTime storageTime;

    /** 仓储温度（℃） */
    private BigDecimal temperature;

    /** 仓储湿度（%） */
    private BigDecimal humidity;

    /** 仓库方电子签名 */
    private String keeperSignature;

    /** 种植方电子签名 */
    private String farmerSignature;

    /** 入库质检报告文件 ID */
    private String qualityReportFileId;

    /** 报告文件哈希 */
    private String reportHash;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---------- 详情接口填充（非表字段） ----------
    /** 种植批次摘要 */
    @TableField(exist = false)
    private RicePlantingBatch plantingBatch;

    /** 地块摘要 */
    @TableField(exist = false)
    private RiceField field;

    /** 入库质检记录 */
    @TableField(exist = false)
    private RiceQualityTest qualityTest;

    /** 链上存证 */
    @TableField(exist = false)
    private RiceChainProof chainProof;
}
