package com.itheima.qukuailian.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 追溯链路结果（需求文档 §九 ZS-01 正反向追溯 + ZS-02 追溯可视化）
 */
@Data
public class TraceChainVO {

    /** 根节点类型 */
    private String rootType;

    /** 根节点业务 ID */
    private String rootId;

    /** 根节点展示名 */
    private String rootLabel;

    /** 链路方向：BACKWARD（反查来源）/FORWARD（正查去向）/BOTH */
    private String direction;

    /** 链路节点（按 stage 顺序） */
    private List<TraceChainNodeVO> nodes;

    /** 节点关系 */
    private List<TraceChainEdgeVO> edges;

    /** 全链路时间轴 */
    private List<TraceTimelineItemVO> timeline;

    /** 汇总指标：节点数、农事记录数、质检数、防伪码数、扫码次数、上链成功率 */
    private Map<String, Object> summary;

    /** 各业务对象的链上存证 */
    private List<Map<String, Object>> chainProofs;

    /** 校验提示（数据缺口、链上摘要不一致、召回冻结等） */
    private List<String> tips;
}
