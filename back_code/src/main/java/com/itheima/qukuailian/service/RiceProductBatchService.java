package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceProductBatch;

/**
 * 成品米批次服务（接口文档 v1.1 §5.22-§5.23）
 */
public interface RiceProductBatchService {

    /** 创建成品米批次：productBatchId 业务唯一 */
    RiceProductBatch create(ProductBatchCreateDTO dto);

    /** 分页查询 */
    IPage<RiceProductBatch> page(long pageNo, long pageSize, String productBatchId, String productName,
                                 String brandName, String status);
}
