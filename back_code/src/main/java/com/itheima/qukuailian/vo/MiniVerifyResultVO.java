package com.itheima.qukuailian.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序真伪鉴别结果（接口文档 v1.1 §6.3）
 */
@Data
public class MiniVerifyResultVO {

    private String traceCode;

    /** 是否正品 */
    private Boolean authentic;

    /** 是否首次扫码 */
    private Boolean firstScan;

    /** 累计扫码次数 */
    private Integer scanCount;

    /** 展示文案：正品 / 该产品已被查询X次 / 存在风险 */
    private String displayText;

    /** 风险等级：LOW/MEDIUM/HIGH */
    private String riskLevel;

    /** 是否触发窜货预警 */
    private Boolean channelWarning;

    /** 链上是否核验通过 */
    private Boolean chainVerified;

    /** 首次扫码时间 */
    private LocalDateTime firstScannedAt;

    /** 本次扫码时间 */
    private LocalDateTime currentScanAt;

    /** 防伪码状态：GENERATED/ACTIVATED/DISABLED/RISK */
    private String codeStatus;
}
