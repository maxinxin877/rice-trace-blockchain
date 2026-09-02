package com.itheima.qukuailian.bcos.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 链上交易结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChainTxResult {

    private String txHash;
    private Long blockNumber;
}
