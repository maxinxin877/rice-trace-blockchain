package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 召回明细（需求文档 §六-4、§九 FC-06 召回处置闭环）
 * <p>通知状态与处置状态分离：通知证明"已告知"，处置证明"实物已回收"。</p>
 */
@Data
@TableName("rice_recall_item")
public class RiceRecallItem {

    @TableId(type = IdType.INPUT)
    private String itemId;

    private String recallTaskId;

    /** PRODUCT_BATCH/TRACE_CODE/GRAIN_BATCH */
    private String itemType;

    /** 成品批次 ID / 防伪码 / 原粮批次 ID */
    private String businessId;

    private String productName;

    /** 关联批次号（便于线下核对实物） */
    private String batchNo;

    private String packageSpec;

    private String expectedRegion;

    private String actualRegion;

    private Integer quantity;

    private BigDecimal weightKg;

    /** PENDING/SENT/CONFIRMED/FAILED */
    private String notifyStatus;

    private String notifyChannel;

    private LocalDateTime notifiedAt;

    /** PENDING/OFF_SHELF/RECOVERED/ISOLATED/RETURNED/SCRAPPED/UNRECOVERED */
    private String handleStatus;

    private String handleRemark;

    private String handledBy;

    private LocalDateTime handledAt;

    /** 是否已冻结（1=禁止出入库与销售） */
    private Boolean frozen;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
