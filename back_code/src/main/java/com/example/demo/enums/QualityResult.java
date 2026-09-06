package com.example.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 质检结果枚举
 */
@Getter
@AllArgsConstructor
public enum QualityResult {

    PASS("合格"),
    FAIL("不合格"),
    PENDING("待检");

    private final String description;
}
