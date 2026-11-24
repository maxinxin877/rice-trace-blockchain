package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包装单元绑定关系（箱 → 单品码 / 托盘 → 箱码）
 */
@Data
@TableName("rice_package_unit_item")
public class RicePackageUnitItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String unitCode;

    private String childCode;

    /** TRACE_CODE/BOX */
    private String childLevel;

    private LocalDateTime createTime;
}
