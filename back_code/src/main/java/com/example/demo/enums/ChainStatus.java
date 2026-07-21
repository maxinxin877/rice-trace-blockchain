package com.example.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 区块链上链状态枚举
 */
@Getter
@AllArgsConstructor
public enum ChainStatus {

    PENDING("待上链"),
    SUCCESS("上链成功"),
    FAILED("上链失败");

    private final String description;
}
