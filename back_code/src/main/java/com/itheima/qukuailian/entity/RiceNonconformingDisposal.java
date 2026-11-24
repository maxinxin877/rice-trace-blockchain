package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 不合格品处置单（需求文档 §九 FC-04）
 * <p>状态机：DRAFT → PENDING_REVIEW → APPROVED → EXECUTED → CLOSED，PENDING_REVIEW 可 REJECTED。</p>
 */
@Data
@TableName("rice_nonconforming_disposal")
public class RiceNonconformingDisposal {

    @TableId(type = IdType.INPUT)
    private String disposalId;

    private String disposalNo;

    /** QUALITY_TEST/SCAN_CHECK/INSPECTION/MANUAL/COMPLAINT */
    private String sourceType;

    private String sourceId;

    /** GRAIN_BATCH/PRODUCT_BATCH/MILLING_BATCH/TRACE_CODE */
    private String businessType;

    private String businessId;

    private String batchNo;

    private String productName;

    /** MOISTURE/IMPURITY/RESIDUE/APPEARANCE/PACKAGE/TEMPERATURE/OTHER */
    private String defectType;

    private String defectDesc;

    private String riskLevel;

    private Integer quantity;

    private BigDecimal weightKg;

    /** ISOLATE/REWORK/REPAIR/SCRAP/CONCESSION/RELEASE */
    private String disposition;

    /** DRAFT/PENDING_REVIEW/APPROVED/REJECTED/EXECUTED/CLOSED */
    private String status;

    private Boolean isolated;

    private String applicant;
    private LocalDateTime appliedAt;
    private String reviewer;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime executedAt;
    private String executeRemark;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
