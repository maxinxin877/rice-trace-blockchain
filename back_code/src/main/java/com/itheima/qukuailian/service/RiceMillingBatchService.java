package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.MillingBatchCompleteDTO;
import com.itheima.qukuailian.dto.MillingBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceMillingBatch;

/**
 * 碾米加工服务（接口文档 v1.1 §5.18-§5.21）
 */
public interface RiceMillingBatchService {

    /** 创建加工批次：原粮批次必须存在且入库质检合格；原粮-成品映射上链 */
    RiceMillingBatch create(MillingBatchCreateDTO dto, String ip);

    /** 分页查询 */
    IPage<RiceMillingBatch> page(long pageNo, long pageSize, String grainBatchId, String productBatchId,
                                 String factoryId, String startTime, String endTime);

    /** 详情：原粮批次 + 成品批次 + 加工参数 + 产出率 + 链上存证 */
    RiceMillingBatch detail(String millingBatchId);

    /** 完成加工：计算产出率、出厂质检上链、产出率异常生成预警、成品批次置 PACKAGED */
    RiceMillingBatch complete(String millingBatchId, MillingBatchCompleteDTO dto, String ip);
}
