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
 * 成品米批次（接口文档 v1.1 §3.9 RiceProductBatch）
 */
@Data
@TableName(value = "rice_product_batch", autoResultMap = true)
public class RiceProductBatch {

    /** 成品米批次 ID（业务唯一，客户端传入） */
    @TableId(type = IdType.INPUT)
    private String productBatchId;

    private String productName;

    /** 水稻品种 */
    private String riceVariety;

    /** 品牌名称 */
    private String brandName;

    /** 包装规格 */
    private String packageSpec;

    /** 执行标准号 */
    private String standardNo;

    /** 营养成分 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> nutritionFacts;

    /** 认证证书文件 ID 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> certificationFileIds;

    /** 预期销售区域 */
    private String expectedSaleRegion;

    /** 状态：DRAFT/PACKAGED/ON_SALE */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
