package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceProductBatch;

import java.util.Map;

/**
 * 成品米批次服务（接口文档 v1.1 §5.22-§5.23）
 */
public interface RiceProductBatchService {

    /** 创建成品米批次：productBatchId 业务唯一，且必须已在碾米加工中创建 */
    RiceProductBatch create(ProductBatchCreateDTO dto);

    /** 分页查询 */
    IPage<RiceProductBatch> page(long pageNo, long pageSize, String productBatchId, String productName,
                                 String brandName, String status);

    /** 溯源：查询成品批次完整链路（成品 → 加工 → 入库 → 种植 → 地块 + 防伪码） */
    Map<String, Object> trace(String productBatchId);
}
