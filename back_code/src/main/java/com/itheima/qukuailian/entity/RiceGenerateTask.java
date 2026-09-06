package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 防伪码生成任务（接口文档 v1.1 §5.24）
 */
@Data
@TableName("rice_generate_task")
public class RiceGenerateTask {

    @TableId(type = IdType.INPUT)
    private String generateTaskId;

    private String productBatchId;

    /** 生成数量 */
    private Integer quantity;

    private String packageSpec;

    private String expectedSaleRegion;

    /** 二维码有效期天数，0 不限 */
    private Integer expireDays;

    /** 状态：PROCESSING/SUCCESS/FAILED */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
