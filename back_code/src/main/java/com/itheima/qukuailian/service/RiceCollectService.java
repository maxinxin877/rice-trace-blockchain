package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.CollectBatchDTO;
import com.itheima.qukuailian.dto.CollectDTO;
import com.itheima.qukuailian.dto.CollectReviewDTO;
import com.itheima.qukuailian.entity.RiceCollectRecord;

import java.util.Map;

/**
 * 扫码采集服务（需求文档 §九 CJ-01/02/05/06/07）
 * <p>一条采集记录 = 一次"扫码 + 自动带出业务数据 + 防错校验"，
 * 无论校验通过还是被拦截都会留痕，便于事后追责与设备异常排查。</p>
 */
public interface RiceCollectService {

    /** 单条采集：自动识别码类型、带出业务快照、执行防错校验 */
    RiceCollectRecord collect(CollectDTO dto, String ip);

    /** 离线补传：逐条幂等入库，返回 accepted/duplicated/failed 统计 */
    Map<String, Object> batchCollect(CollectBatchDTO dto, String ip);

    /** 人工补录审核 */
    RiceCollectRecord review(String collectId, CollectReviewDTO dto, String ip);

    /** 分页查询采集记录 */
    IPage<RiceCollectRecord> page(long pageNo, long pageSize, String code, String action,
                                  String terminalType, String source, String status,
                                  String checkResult, String startTime, String endTime);

    /** 采集概览：今日采集量、拦截量、待审核量、按动作分布 */
    Map<String, Object> stats();
}
