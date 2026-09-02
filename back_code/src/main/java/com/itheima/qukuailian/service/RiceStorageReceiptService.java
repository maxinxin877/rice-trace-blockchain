package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.QualityTestSubmitDTO;
import com.itheima.qukuailian.dto.StorageReceiptCreateDTO;
import com.itheima.qukuailian.entity.RiceQualityTest;
import com.itheima.qukuailian.entity.RiceStorageReceipt;

/**
 * 收储入库服务（接口文档 v1.1 §5.14-§5.17）
 */
public interface RiceStorageReceiptService {

    /** 创建入库单：grainBatchId 唯一；创建后种植批次状态更新为 STORED；入库单摘要上链 */
    RiceStorageReceipt create(StorageReceiptCreateDTO dto, String ip);

    /** 分页查询 */
    IPage<RiceStorageReceipt> page(long pageNo, long pageSize, String grainBatchId, String plantingBatchId,
                                   String warehouseId, String grainGrade, String startTime, String endTime);

    /** 详情：种植批次摘要 + 地块摘要 + 入库质检 + 仓储环境 + 链上存证 */
    RiceStorageReceipt detail(String storageReceiptId);

    /** 提交入库质检：报告 SHA-256 + 质检摘要上链 */
    RiceQualityTest submitQualityTest(String storageReceiptId, QualityTestSubmitDTO dto);
}
