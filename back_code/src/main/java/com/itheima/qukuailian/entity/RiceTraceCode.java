package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 防伪码（接口文档 v1.1 §3.6 RiceTraceCode）
 */
@Data
@TableName("rice_trace_code")
public class RiceTraceCode {

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 防伪码（唯一） */
    private String traceCode;

    /** 二维码图片 URL */
    private String qrCodeUrl;

    /** 成品米批次 ID */
    private String productBatchId;

    /** 包装规格 */
    private String packageSpec;

    /** 码状态：GENERATED/ACTIVATED/DISABLED/RISK */
    private String status;

    /** 二维码有效期天数，0 不限 */
    private Integer expireDays;

    private LocalDateTime activatedAt;

    private LocalDateTime firstScannedAt;

    /** 累计扫码次数 */
    private Integer scanCount;

    /** 预期销售区域 */
    private String expectedSaleRegion;

    /** 最近扫码区域 */
    private String lastScanRegion;

    /** 风险等级：LOW/MEDIUM/HIGH */
    private String riskLevel;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 产品名称（非表字段，列表查询时填充，供前端展示） */
    @TableField(exist = false)
    private String productName;
}
