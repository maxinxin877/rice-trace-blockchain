package com.example.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 溯源码状态枚举
 */
@Getter
@AllArgsConstructor
public enum TraceCodeStatus {

    GENERATED("已生成"),
    ACTIVATED("已激活"),
    DISABLED("已停用"),
    RISK("风险码");

    private final String description;
}
