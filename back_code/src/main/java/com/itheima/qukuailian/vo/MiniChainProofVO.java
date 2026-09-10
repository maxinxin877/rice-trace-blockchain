package com.itheima.qukuailian.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序链上核验结果（接口文档 v1.1 §6.4）
 */
@Data
public class MiniChainProofVO {

    private String traceCode;

    /** 是否核验通过 */
    private Boolean verified;

    private String productBatchId;

    /** 当前页面数据摘要 */
    private String dataHash;

    /** 链上摘要 */
    private String chainHash;

    private String txId;

    private Long blockHeight;

    private LocalDateTime chainTime;
}
