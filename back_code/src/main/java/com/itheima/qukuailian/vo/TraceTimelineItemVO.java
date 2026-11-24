package com.itheima.qukuailian.vo;

import lombok.Data;

import java.util.List;

/**
 * 追溯时间轴事件（需求文档 §九 ZS-02：时间轴展示完整履历）
 */
@Data
public class TraceTimelineItemVO {

    private String time;

    private String stage;

    private String title;

    private String description;

    private String businessType;

    private String businessId;

    private String organization;

    private String chainStatus;

    private String txId;

    /** 关联凭证文件 ID，前端可预览检测报告/照片 */
    private List<String> fileIds;

    /** 是否为风险事件（不合格、召回、冻结） */
    private Boolean risky;
}
