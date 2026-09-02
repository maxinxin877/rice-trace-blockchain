package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 质检记录（接口文档 v1.1 §3.8 RiceQualityTest）
 */
@Data
@TableName("rice_quality_test")
public class RiceQualityTest {

    @TableId(type = IdType.INPUT)
    private String qualityTestId;

    /** 质检归属：STORAGE/MILLING */
    private String businessType;

    /** 入库单 ID 或加工批次 ID */
    private String businessId;

    /** 检测机构 */
    private String testAgency;

    private LocalDateTime testTime;

    /** 总体结果：PASS/FAIL/PENDING */
    private String overallResult;

    /** 质检报告文件 ID */
    private String reportFileId;

    /** 报告 SHA-256 */
    private String reportHash;

    private String remark;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;

    /** 质检项目明细（非表字段，查询时填充） */
    @TableField(exist = false)
    private List<RiceQualityTestItem> items;
}
