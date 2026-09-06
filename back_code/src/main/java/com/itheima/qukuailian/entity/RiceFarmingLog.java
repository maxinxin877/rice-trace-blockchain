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

/**
 * 农事记录（接口文档 v1.1 §3.3 RiceFarmingLog）
 */
@Data
@TableName(value = "rice_farming_log", autoResultMap = true)
public class RiceFarmingLog {

    @TableId(type = IdType.INPUT)
    private String logId;

    private String plantingBatchId;

    /** 农事类型：SOWING/FERTILIZATION/PESTICIDE/IRRIGATION/WEEDING/HARVEST */
    private String operationType;

    private LocalDateTime operationTime;

    private String operatorId;
    private String operatorName;

    /** 投入品名称 */
    private String materialName;

    private String materialBatchNo;

    /** 使用量 */
    private BigDecimal materialDosage;

    private String materialUnit;

    /** 安全间隔期 */
    private Integer safeIntervalDays;

    private String description;

    /** 现场照片/采购凭证文件 ID 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> proofFileIds;

    /** 记录摘要哈希 */
    private String dataHash;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
}
