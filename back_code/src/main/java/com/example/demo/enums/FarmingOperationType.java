package com.example.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 农事操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum FarmingOperationType {

    SOWING("播种"),
    FERTILIZATION("施肥"),
    PESTICIDE("用药"),
    IRRIGATION("灌溉"),
    WEEDING("除草"),
    HARVEST("收割");

    private final String description;
}
