package com.itheima.qukuailian.vo;

import lombok.Data;

/**
 * 防错校验单条结果（需求文档 §九 FC-01~FC-05、§8.4 生产防错体系）
 */
@Data
public class ComplianceCheckVO {

    /** 校验项编码：BATCH_LOCKED/RECALL_FROZEN/WITHDRAWAL_PERIOD/QUALITY_FAIL/TRACE_CODE_STATUS */
    private String ruleCode;

    /** 校验项名称 */
    private String ruleName;

    /** 是否通过 */
    private Boolean passed;

    /** 校验结论说明 */
    private String message;

    /** 阻断级别：BLOCK（禁止继续流转）/WARN（告警但放行） */
    private String level;

    /** 命中的业务对象（批次/防伪码） */
    private String businessId;
}
