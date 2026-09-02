package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.FarmingLogCreateDTO;
import com.itheima.qukuailian.entity.RiceFarmingLog;

/**
 * 农事记录服务（接口文档 v1.1 §5.10-§5.11）
 */
public interface RiceFarmingLogService {

    /**
     * 新增农事记录：计算记录摘要并上链（农药类强制投入品信息）
     */
    RiceFarmingLog create(String plantingBatchId, FarmingLogCreateDTO dto);

    /** 分页查询 */
    IPage<RiceFarmingLog> page(String plantingBatchId, String operationType,
                               String startTime, String endTime, long pageNo, long pageSize);
}
