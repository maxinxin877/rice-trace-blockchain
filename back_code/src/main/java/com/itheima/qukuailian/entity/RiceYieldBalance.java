package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 产量平衡校验结果（接口文档 v1.1 §7.1-§7.2）
 */
@Data
@TableName(value = "rice_yield_balance", autoResultMap = true)
public class RiceYieldBalance {

    @TableId(type = IdType.INPUT)
    private String checkId;

    /** 校验范围：PLANTING_TO_STORAGE/STORAGE_TO_MILLING/FULL_CHAIN */
    private String checkScope;

    private String plantingBatchId;
    private String grainBatchId;
    private String productBatchId;

    /** 结果：PASS/FAIL/PENDING */
    private String result;

    /** 校验明细（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> items;

    private String chainStatus;
    private String txId;
    private Long blockHeight;
    private LocalDateTime chainTime;
    private String chainError;

    private LocalDateTime createTime;
}
