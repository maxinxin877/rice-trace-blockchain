package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志（接口文档 v1.1 §3.12 RiceAuditLog）
 */
@Data
@TableName("rice_audit_log")
public class RiceAuditLog {

    @TableId(type = IdType.INPUT)
    private String auditId;

    /** 业务类型：FIELD/PLANTING_BATCH/FARMING_LOG/... */
    private String businessType;

    private String businessId;

    /** 操作类型：CREATE/UPDATE/DELETE/VERIFY/HANDLE */
    private String operationType;

    private String operatorId;
    private String operatorName;

    /** 修改前摘要 */
    private String beforeHash;

    /** 修改后摘要 */
    private String afterHash;

    /** 修改原因 */
    private String reason;

    private String ip;

    private LocalDateTime operationTime;

    /** 审计摘要上链交易 ID */
    private String txId;

    private LocalDateTime createTime;
}
