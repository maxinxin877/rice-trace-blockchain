package com.itheima.qukuailian.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 追溯链路关系边（需求文档 §九 ZS-02：上下游关系图 / 批次拆分合并树）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceChainEdgeVO {

    /** 上游节点 nodeKey */
    private String from;

    /** 下游节点 nodeKey */
    private String to;

    /** 关系类型：BOUND_TO/HARVESTED_AS/STORED_AS/MILLED_INTO/PACKED_AS/ACTIVATED_AS */
    private String relation;

    /** 关系说明（前端连线标签） */
    private String label;

    /** 数量/重量等流转量（含单位，便于展示"1000kg → 680kg"） */
    private String quantity;
}
