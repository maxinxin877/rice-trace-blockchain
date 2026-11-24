package com.itheima.qukuailian.service;

import java.util.Map;

/**
 * 质量分析服务（需求文档 §九 FX-01~FX-04、§8.6 数据分析辅助决策）
 * <p>先不引入模型，全部用可解释的聚合与规则打分，保证答辩/评审时每个数字都能说清来源。</p>
 */
public interface RiceAnalyticsService {

    /** 综合分析概览：质量趋势 + 异常分布 + 召回绩效 + 风险评分 */
    Map<String, Object> overview();
}
