package com.itheima.qukuailian.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 窜货预警（前端"窜货预警"页面展示结构）
 * <p>由 {@code rice_risk_warning} 记录 + 关联防伪码/成品批次信息组装而成。</p>
 */
@Data
public class ChannelWarningVO {

    private String warningId;

    /** 防伪码 */
    private String traceCode;

    /** 产品名称 */
    private String productName;

    /** 预期销售区域 */
    private String expectedRegion;

    /** 实际扫码区域 */
    private String actualRegion;

    /** 累计扫码次数 */
    private Integer scanCount;

    /** 风险等级：LOW/MEDIUM/HIGH */
    private String riskLevel;

    /** 预警时间 */
    private LocalDateTime warningTime;

    /** 是否已处理 */
    private Boolean handled;

    /** 预警内容 */
    private String warningContent;
}
