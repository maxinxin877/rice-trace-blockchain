package com.itheima.qukuailian.vo;

import lombok.Data;

import java.util.List;

/**
 * 追溯入口识别结果（需求文档 §九 ZS-01：支持按单品码/批次号等多种入口检索）
 */
@Data
public class TraceResolveVO {

    /** 原始关键词 */
    private String keyword;

    /** 识别出的类型：TRACE_CODE/PRODUCT_BATCH/MILLING_BATCH/GRAIN_BATCH/STORAGE_RECEIPT/PLANTING_BATCH/FIELD */
    private String matchedType;

    /** 识别出的业务 ID */
    private String matchedId;

    /** 展示名 */
    private String label;

    /** 补充说明（如"该码所属成品批次 PASTA..."） */
    private String note;

    /** 其他可能的匹配入口（同码命中多条时） */
    private List<TraceResolveVO> alternatives;
}
