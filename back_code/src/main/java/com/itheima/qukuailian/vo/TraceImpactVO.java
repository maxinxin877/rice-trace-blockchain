package com.itheima.qukuailian.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 影响范围分析结果（需求文档 §六-4：自动计算风险影响范围，生成涉及产品/订单/门店/数量/重量清单）
 * <p>同时作为召回任务创建时的范围计算依据。</p>
 */
@Data
public class TraceImpactVO {

    private String rootType;

    private String rootId;

    private String rootLabel;

    /** 受影响原粮批次（grainBatchId） */
    private List<String> grainBatches;

    /** 受影响加工批次 */
    private List<String> millingBatches;

    /** 受影响成品批次明细 */
    private List<Map<String, Object>> productBatches;

    /** 受影响防伪码总数 */
    private Integer traceCodeCount;

    /** 受影响防伪码样例（最多 50 条，避免响应过大） */
    private List<String> traceCodeSamples;

    /** 受影响产品件数（防伪码即一物一码，等于件数） */
    private Integer quantity;

    /** 受影响重量（kg） */
    private BigDecimal weightKg;

    /** 扫码区域分布：region → 扫码次数 */
    private List<Map<String, Object>> regions;

    /** 已售/已扫码的防伪码数量（风险最高） */
    private Integer scannedCodeCount;

    /** 已冻结数量 */
    private Integer frozenCodeCount;

    /** 计算说明与风险提示 */
    private List<String> tips;
}
