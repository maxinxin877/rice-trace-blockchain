package com.itheima.qukuailian.service;

import com.itheima.qukuailian.vo.TraceChainVO;
import com.itheima.qukuailian.vo.TraceImpactVO;
import com.itheima.qukuailian.vo.TraceResolveVO;

/**
 * 追溯查询服务（需求文档 §六-3 数据检索与正反向追溯、§九 ZS-01/ZS-02）
 * <p>统一入口：任意一个业务标识（单品码 / 成品批次 / 加工批次 / 原粮批次 / 入库单 /
 * 种植批次）都能还原出"地块 → 种植 → 收储 → 加工 → 包装 → 一物一码 → 扫码流通"的完整链路。</p>
 */
public interface RiceTraceQueryService {

    /**
     * 识别关键词类型（扫码枪/手工输入的统一入口）
     *
     * @param keyword 单品码、批次号、入库单号等
     */
    TraceResolveVO resolve(String keyword);

    /**
     * 构建追溯链路
     *
     * @param businessType TRACE_CODE/PRODUCT_BATCH/MILLING_BATCH/GRAIN_BATCH/STORAGE_RECEIPT/PLANTING_BATCH/FIELD
     * @param businessId   对应业务 ID
     * @param direction    BACKWARD（反查来源）/FORWARD（正查去向）/BOTH
     */
    TraceChainVO chain(String businessType, String businessId, String direction);

    /**
     * 影响范围分析：从问题批次正向定位受影响的成品、防伪码、区域与数量重量
     * <p>召回任务创建时直接复用该结果。</p>
     */
    TraceImpactVO impact(String businessType, String businessId);
}
