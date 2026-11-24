package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 箱码 / 托盘码（需求文档 §九 YM-01 统一编码体系）
 */
@Data
@TableName("rice_package_unit")
public class RicePackageUnit {

    @TableId(type = IdType.INPUT)
    private String unitCode;

    /** BOX/PALLET */
    private String unitLevel;

    /** 上级码（箱→托盘），空表示顶层 */
    private String parentCode;

    private String productBatchId;

    private String packageSpec;

    /** 子级层级：TRACE_CODE/BOX */
    private String childLevel;

    private Integer childCount;

    /** 折算单品数量 */
    private Integer totalQuantity;

    /** SEALED/ACTIVATED/DISABLED */
    private String status;

    private String expectedSaleRegion;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
