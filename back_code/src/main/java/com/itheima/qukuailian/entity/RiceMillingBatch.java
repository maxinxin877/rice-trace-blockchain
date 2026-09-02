package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 碾米加工批次（接口文档 v1.1 §3.5 RiceMillingBatch）
 */
@Data
@TableName(value = "rice_milling_batch", autoResultMap = true)
public class RiceMillingBatch {

    @TableId(type = IdType.INPUT)
    private String millingBatchId;

    /** 原粮批次 ID */
    private String grainBatchId;

    /** 成品米批次 ID */
    private String productBatchId;

    /** 加工厂 ID */
    private String factoryId;

    /** 稻谷出库量（kg） */
    private BigDecimal grainOutWeightKg;

    /** 精米产出量（kg） */
    private BigDecimal riceOutputWeightKg;

    /** 精米产出率（%） */
    private BigDecimal yieldRate;

    private LocalDateTime processStartTime;
    private LocalDateTime processEndTime;

    /** 脱壳/抛光/色选/包装参数 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> processParams;

    /** 出厂质检摘要 */
    private String qualitySummary;

    /** 出厂质检报告文件 ID */
    private String qualityReportFileId;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---------- 详情接口填充（非表字段） ----------
    /** 成品米批次 */
    @TableField(exist = false)
    private RiceProductBatch productBatch;

    /** 链上存证 */
    @TableField(exist = false)
    private RiceChainProof chainProof;
}
