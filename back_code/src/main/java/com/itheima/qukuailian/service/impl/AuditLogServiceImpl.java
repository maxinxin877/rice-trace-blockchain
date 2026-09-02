package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.qukuailian.entity.RiceAuditLog;
import com.itheima.qukuailian.mapper.RiceAuditLogMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.utils.IdGen;
import com.itheima.qukuailian.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final RiceAuditLogMapper auditLogMapper;

    @Override
    public void record(String businessType, String businessId, String operationType,
                       String beforeHash, String afterHash, String reason, String ip) {
        RiceAuditLog logRecord = new RiceAuditLog();
        logRecord.setAuditId(IdGen.generate("AUD"));
        logRecord.setBusinessType(businessType);
        logRecord.setBusinessId(businessId);
        logRecord.setOperationType(operationType);
        logRecord.setOperatorId(String.valueOf(UserContext.getUserId()));
        logRecord.setOperatorName(UserContext.getUsername());
        logRecord.setBeforeHash(beforeHash);
        logRecord.setAfterHash(afterHash);
        logRecord.setReason(reason);
        logRecord.setIp(ip == null ? "" : ip);
        logRecord.setOperationTime(LocalDateTime.now());
        auditLogMapper.insert(logRecord);
        log.info("审计日志: business={}/{}, op={}, reason={}",
                businessType, businessId, operationType, reason);
    }

    @Override
    public IPage<RiceAuditLog> page(long pageNo, long pageSize, String businessType, String businessId,
                                    String operatorId, String operationType, String startTime, String endTime) {
        LambdaQueryWrapper<RiceAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(businessType), RiceAuditLog::getBusinessType, businessType)
                .eq(StringUtils.hasText(businessId), RiceAuditLog::getBusinessId, businessId)
                .eq(StringUtils.hasText(operatorId), RiceAuditLog::getOperatorId, operatorId)
                .eq(StringUtils.hasText(operationType), RiceAuditLog::getOperationType, operationType)
                .ge(StringUtils.hasText(startTime), RiceAuditLog::getOperationTime, LocalDateTime.parse(startTime))
                .le(StringUtils.hasText(endTime), RiceAuditLog::getOperationTime, LocalDateTime.parse(endTime))
                .orderByDesc(RiceAuditLog::getOperationTime);
        return auditLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }
}
