package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 扫码日志（接口文档 v1.1 §3.10 RiceScanLog）
 */
@Data
@TableName("rice_scan_log")
public class RiceScanLog {

    @TableId(type = IdType.INPUT)
    private String scanLogId;

    /** 防伪码 */
    private String traceCode;

    /** 成品米批次 ID */
    private String productBatchId;

    private LocalDateTime scanTime;

    /** 扫码经度 */
    private BigDecimal lng;

    /** 扫码纬度 */
    private BigDecimal lat;

    /** 扫码地区 */
    private String region;

    /** 设备指纹 */
    private String deviceId;

    /** 微信 openid */
    private String openid;

    /** 扫码场景 */
    private String scene;

    /** 是否正品 */
    private Boolean authentic;

    /** 是否首次扫码 */
    private Boolean firstScan;

    /** 风险等级 */
    private String riskLevel;

    private LocalDateTime createTime;
}
