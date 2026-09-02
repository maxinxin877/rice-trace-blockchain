package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 质检项目明细（接口文档 v1.1 §3.8 items[]）
 */
@Data
@TableName("rice_quality_test_item")
public class RiceQualityTestItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String qualityTestId;

    /** 检测项目 */
    private String itemName;

    /** 检测值 */
    private BigDecimal itemValue;

    private String unit;

    /** 标准值 */
    private String standardValue;

    /** 单项结果：PASS/FAIL */
    private String result;
}
