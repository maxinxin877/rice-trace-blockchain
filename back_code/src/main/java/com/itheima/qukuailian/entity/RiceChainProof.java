package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 链上存证（接口文档 v1.1 §3.13 RiceChainProof）
 */
@Data
@TableName(value = "rice_chain_proof", autoResultMap = true)
public class RiceChainProof {

    @TableId(type = IdType.INPUT)
    private String chainProofId;

    /** 业务类型：FIELD/PLANTING_BATCH/FARMING_LOG/STORAGE_RECEIPT/QUALITY_TEST/MILLING_BATCH/TRACE_CODE/RISK_WARNING/YIELD_BALANCE/AUDIT_LOG */
    private String businessType;

    private String businessId;

    /** 业务摘要哈希 */
    private String dataHash;

    /** 关联文件哈希数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> fileHashes;

    /** 区块链交易 ID */
    private String txId;

    private Long blockHeight;

    private LocalDateTime chainTime;

    /** 上链状态：PENDING/SUCCESS/FAILED */
    private String chainStatus;

    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
