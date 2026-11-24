package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 扫码采集记录（需求文档 §九 CJ-01/02/05/06/07）
 */
@Data
@TableName(value = "rice_collect_record", autoResultMap = true)
public class RiceCollectRecord {

    @TableId(type = IdType.INPUT)
    private String collectId;

    /** 幂等键：终端生成（码 + 动作 + 时间戳），唯一索引兜底防重 */
    private String idempotencyKey;

    private String collectCode;

    /** TRACE_CODE/BOX_CODE/PALLET_CODE/PRODUCT_BATCH/GRAIN_BATCH/PLANTING_BATCH/STORAGE_RECEIPT */
    private String codeType;

    /** STORE/MILL/ACTIVATE/OUTBOUND/SHELF/SELL/INSPECT */
    private String action;

    /** SCANNER/PDA/MOBILE/WEB */
    private String terminalType;

    private String operatorName;

    private LocalDateTime collectTime;

    /** ONLINE/OFFLINE/MANUAL */
    private String source;

    private String businessType;

    private String businessId;

    /** 扫码后自动带出的业务数据 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> snapshot;

    /** PASS/WARN/BLOCK */
    private String checkResult;

    private String checkMessage;

    /** 幂等命中（重复提交） */
    private Boolean duplicated;

    /** COLLECTED/PENDING_REVIEW/APPROVED/REJECTED */
    private String status;

    /** 人工补录原因 */
    private String reviewReason;

    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
