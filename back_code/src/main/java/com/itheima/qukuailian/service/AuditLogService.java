package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.entity.RiceAuditLog;

/**
 * 审计日志服务：业务数据修改留痕（接口文档 v1.1 §7.5）
 */
public interface AuditLogService {

    /**
     * 记录一条审计日志
     *
     * @param businessType  业务类型：FIELD/PLANTING_BATCH/FARMING_LOG/STORAGE_RECEIPT/...
     * @param businessId    业务 ID
     * @param operationType 操作类型：CREATE/UPDATE/DELETE/VERIFY/HANDLE
     * @param beforeHash    修改前摘要（可空）
     * @param afterHash     修改后摘要（可空）
     * @param reason        修改原因
     * @param ip            操作 IP（可空）
     */
    void record(String businessType, String businessId, String operationType,
                String beforeHash, String afterHash, String reason, String ip);

    /** 分页查询审计日志 */
    IPage<RiceAuditLog> page(long pageNo, long pageSize, String businessType, String businessId,
                             String operatorId, String operationType, String startTime, String endTime);
}
