package com.example.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 水稻批次状态枚举
 */
@Getter
@AllArgsConstructor
public enum RiceBatchStatus {

    DRAFT("草稿"),
    PLANTED("已播种"),
    HARVESTED("已收割"),
    STORED("已入库"),
    MILLING("加工中"),
    PACKAGED("已包装"),
    ON_SALE("已上市"),
    LOCKED("监管锁定");

    private final String description;
}
