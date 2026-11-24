package com.itheima.qukuailian.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 追溯链路节点（需求文档 §九 ZS-02 追溯可视化）
 * <p>一个节点 = 链路上的一次关键状态变更，携带业务摘要、凭证文件与链上存证信息。</p>
 */
@Data
public class TraceChainNodeVO {

    /** 节点唯一键：{nodeType}:{businessId} */
    private String nodeKey;

    /** 节点类型：FIELD/PLANTING_BATCH/FARMING_LOG/STORAGE_RECEIPT/MILLING_BATCH/PRODUCT_BATCH/TRACE_CODE */
    private String nodeType;

    /** 环节名：种植/收储/加工/包装/流通 */
    private String stage;

    /** 节点标题（页面卡片主标题） */
    private String title;

    /** 业务类型（对应 rice_chain_proof.business_type） */
    private String businessType;

    /** 业务 ID（点击可跳转详情） */
    private String businessId;

    /** 业务发生时间（ISO 字符串） */
    private String time;

    /** 责任主体：种植户/仓库/加工厂/品牌方 */
    private String organization;

    /** 地点：地块坐标、仓库编号、加工厂 */
    private String location;

    /** 一句话摘要 */
    private String summary;

    /** 业务状态（批次状态/防伪码状态等原始值） */
    private String status;

    /** 链上状态：PENDING/SUCCESS/FAILED */
    private String chainStatus;

    private String txId;

    private String dataHash;

    /** 关键指标（重量、产出率、水分、扫码次数等，前端按 key-value 展示） */
    private Map<String, Object> metrics;

    /** 关联凭证文件 ID */
    private List<String> fileIds;

    /** 是否存在风险（召回冻结、质检不合格等） */
    private Boolean risky;

    /** 风险说明 */
    private String riskNote;

    /** 前端路由（管理端详情页跳转地址，可空） */
    private String detailRoute;
}
