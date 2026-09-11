package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.TraceCodeActivateDTO;
import com.itheima.qukuailian.dto.TraceCodeGenerateDTO;
import com.itheima.qukuailian.entity.RiceGenerateTask;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.vo.ChannelWarningVO;

import java.util.Map;

/**
 * 品牌防伪服务（接口文档 v1.1 §5.24-§5.27）
 */
public interface RiceTraceCodeService {

    /** 批量生成防伪码（默认状态 GENERATED，生成批次摘要上链） */
    RiceGenerateTask generate(TraceCodeGenerateDTO dto, String ip);

    /** 激活防伪码（仅 GENERATED 可激活；激活状态摘要上链；成品批次置 ON_SALE） */
    Map<String, Object> activate(TraceCodeActivateDTO dto, String ip);

    /** 分页查询防伪码（附带产品名称，供列表展示） */
    IPage<RiceTraceCode> page(long pageNo, long pageSize, String productBatchId, String traceCode,
                              String status, String riskLevel);

    /** 查询窜货预警（CHANNEL_CONFLICT / REPEAT_SCAN），返回前端页面所需结构 */
    IPage<ChannelWarningVO> pageChannelWarnings(long pageNo, long pageSize, String productBatchId,
                                                String traceCode, String riskLevel, String region,
                                                String startTime, String endTime);
}
