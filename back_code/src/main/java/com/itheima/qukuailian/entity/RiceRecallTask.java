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
import java.util.Map;

/**
 * 召回任务（需求文档 §六-4 问题追溯与召回管理、§九 FC-05/FC-06/JK-06）
 * <p>状态机：DRAFT → SUBMITTED → APPROVED → RELEASED → HANDLING → CLOSED，
 * SUBMITTED 可 REJECTED，未发布可 CANCELLED。</p>
 */
@Data
@TableName(value = "rice_recall_task", autoResultMap = true)
public class RiceRecallTask {

    @TableId(type = IdType.INPUT)
    private String recallTaskId;

    /** 召回编号（对外公示，格式 RC-20261102-0001） */
    private String recallNo;

    /** 触发类型：QUALITY_FAIL/CHANNEL_RISK/COMPLAINT/REGULATOR/ENTERPRISE */
    private String triggerType;

    private String triggerBusinessType;

    private String triggerBusinessId;

    /** 风险等级：LOW/MEDIUM/HIGH */
    private String riskLevel;

    private String recallReason;

    private Integer affectedBatchCount;

    private Integer affectedCodeCount;

    private BigDecimal affectedWeightKg;

    /** 受影响销售区域，逗号分隔 */
    private String affectedRegions;

    /** DRAFT/SUBMITTED/APPROVED/RELEASED/HANDLING/CLOSED/REJECTED/CANCELLED */
    private String status;

    private LocalDateTime deadline;

    private String initiatedBy;
    private LocalDateTime initiatedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String approveComment;
    private LocalDateTime releasedAt;
    private LocalDateTime closedAt;
    private String closeResult;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---------- 详情接口填充（非表字段） ----------

    /** 召回明细 */
    @TableField(exist = false)
    private List<RiceRecallItem> items;

    /** 处置进度统计 */
    @TableField(exist = false)
    private Map<String, Object> progress;

    /** 触发对象摘要（如不合格质检单所属批次） */
    @TableField(exist = false)
    private Map<String, Object> triggerDetail;
}
