package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.PlantingBatchCreateDTO;
import com.itheima.qukuailian.dto.PlantingBatchUpdateDTO;
import com.itheima.qukuailian.entity.RicePlantingBatch;

/**
 * 种植批次服务（接口文档 v1.1 §5.6-§5.9）
 */
public interface RicePlantingBatchService {

    /** 创建种植批次：同一地块同时仅允许一个未结束批次，创建后状态为 PLANTED */
    RicePlantingBatch create(PlantingBatchCreateDTO dto, String ip);

    /** 分页查询 */
    IPage<RicePlantingBatch> page(long pageNo, long pageSize, String fieldId, String riceVariety,
                                  String status, String sowingDateStart, String sowingDateEnd);

    /** 详情：地块摘要 + 农事/环境数据摘要 + 收储状态 + 链上存证状态 */
    RicePlantingBatch detail(String plantingBatchId);

    /** 更新：完整覆盖 + 审计原因；已入库批次禁止修改确地确种信息 */
    RicePlantingBatch update(String plantingBatchId, PlantingBatchUpdateDTO dto, String ip);
}
