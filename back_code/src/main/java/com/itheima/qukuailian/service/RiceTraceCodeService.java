package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.TraceCodeActivateDTO;
import com.itheima.qukuailian.dto.TraceCodeGenerateDTO;
import com.itheima.qukuailian.entity.RiceGenerateTask;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceTraceCode;

import java.util.Map;

/**
 * 品牌防伪服务（接口文档 v1.1 §5.24-§5.27）
 */
public interface RiceTraceCodeService {

    /** 批量生成防伪码（默认状态 GENERATED，生成批次摘要上链） */
    RiceGenerateTask generate(TraceCodeGenerateDTO dto, String ip);

    /** 激活防伪码（仅 GENERATED 可激活；激活状态摘要上链；成品批次置 ON_SALE） */
    Map<String, Object> activate(TraceCodeActivateDTO dto, String ip);

    /** 分页查询防伪码 */
    IPage<RiceTraceCode> page(long pageNo, long pageSize, String productBatchId, String traceCode,
                              String status, String riskLevel);

    /** 查询窜货预警（CHANNEL_CONFLICT / REPEAT_SCAN） */
    IPage<RiceRiskWarning> pageChannelWarnings(long pageNo, long pageSize, String productBatchId,
                                               String traceCode, String riskLevel, String region,
                                               String startTime, String endTime);

    /** 消费者扫码：按防伪码组装全链路溯源详情；防伪码不存在时返回 null */
    Map<String, Object> traceDetail(String traceCode);

    /** 消费者扫码：按防伪码查询链上核验结果（存证表无记录时用码自身上链字段兜底） */
    Map<String, Object> chainProof(String traceCode);

    /** 消费者扫码验真：记录扫码日志、累计次数，识别首次/重复/窜货/无效 */
    Map<String, Object> verifyCode(String traceCode, String region);
}
