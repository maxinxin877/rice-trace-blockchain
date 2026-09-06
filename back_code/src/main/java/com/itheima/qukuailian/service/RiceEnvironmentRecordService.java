package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.EnvironmentRecordCreateDTO;
import com.itheima.qukuailian.entity.RiceEnvironmentRecord;

/**
 * 稻田环境数据服务（接口文档 v1.1 §5.12-§5.13）
 */
public interface RiceEnvironmentRecordService {

    /** 上传环境数据（IOT/DRONE/MANUAL） */
    RiceEnvironmentRecord create(String plantingBatchId, EnvironmentRecordCreateDTO dto);

    /** 分页查询 */
    IPage<RiceEnvironmentRecord> page(String plantingBatchId, String sourceType,
                                      String startTime, String endTime, long pageNo, long pageSize);
}
