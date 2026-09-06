package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险预警（接口文档 v1.1 §3.11 RiceRiskWarning）
 */
@Data
@TableName("rice_risk_warning")
public class RiceRiskWarning {

    @TableId(type = IdType.INPUT)
    private String warningId;

    /** 预警类型：YIELD_BALANCE/CHANNEL_CONFLICT/REPEAT_SCAN/CHAIN_VERIFY_FAILED */
    private String warningType;

    /** 风险等级：LOW/MEDIUM/HIGH */
    private String riskLevel;

    /** 关联业务类型 */
    private String businessType;

    /** 关联业务 ID */
    private String businessId;

    private String warningContent;

    /** 是否已处理 */
    private Boolean handled;

    private String handledBy;
    private LocalDateTime handledAt;
    private String handleResult;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
