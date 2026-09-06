package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.FarmingLogCreateDTO;
import com.itheima.qukuailian.entity.FileResource;
import com.itheima.qukuailian.entity.RiceFarmingLog;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.mapper.FileResourceMapper;
import com.itheima.qukuailian.mapper.RiceFarmingLogMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceFarmingLogService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import com.itheima.qukuailian.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceFarmingLogServiceImpl extends ServiceImpl<RiceFarmingLogMapper, RiceFarmingLog>
        implements RiceFarmingLogService {

    private final RicePlantingBatchMapper plantingBatchMapper;
    private final FileResourceMapper fileResourceMapper;
    private final ChainProofService chainProofService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceFarmingLog create(String plantingBatchId, FarmingLogCreateDTO dto) {
        RicePlantingBatch batch = plantingBatchMapper.selectById(plantingBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "种植批次不存在: " + plantingBatchId);
        }
        // 农药类农事记录：投入品信息必填（接口文档 §5.10）
        if ("PESTICIDE".equals(dto.getOperationType())
                && (!StringUtils.hasText(dto.getMaterialName())
                || dto.getMaterialDosage() == null
                || !StringUtils.hasText(dto.getMaterialUnit())
                || dto.getSafeIntervalDays() == null)) {
            throw new BizException(ResultCode.VALIDATION_FAILED.getCode(),
                    "农药类农事记录必须填写投入品名称、用量、单位和安全间隔期");
        }

        RiceFarmingLog logRecord = new RiceFarmingLog();
        BeanUtils.copyProperties(dto, logRecord);
        logRecord.setLogId(IdGen.generate("FLOG"));
        logRecord.setPlantingBatchId(plantingBatchId);
        // 操作人：接口传入 operatorId，姓名取当前登录用户
        logRecord.setOperatorName(UserContext.getUsername());
        // 记录摘要（参与上链）
        logRecord.setDataHash(computeDataHash(logRecord));
        logRecord.setChainStatus("PENDING");
        save(logRecord);

        // 投入品采购凭证文件 SHA-256 指纹参与上链（接口文档 §5.10）
        List<String> fileHashes = fileHashesOf(dto.getProofFileIds());
        chainProofService.submit("FARMING_LOG", logRecord.getLogId(), logRecord.getDataHash(), fileHashes);
        log.info("农事记录创建成功: logId={}, batch={}, type={}",
                logRecord.getLogId(), plantingBatchId, dto.getOperationType());
        return logRecord;
    }

    @Override
    public IPage<RiceFarmingLog> page(String plantingBatchId, String operationType,
                                      String startTime, String endTime, long pageNo, long pageSize) {
        LambdaQueryWrapper<RiceFarmingLog> wrapper = new LambdaQueryWrapper<>();
        LocalDateTime start = StringUtils.hasText(startTime) ? LocalDateTime.parse(startTime) : null;
        LocalDateTime end = StringUtils.hasText(endTime) ? LocalDateTime.parse(endTime) : null;
        wrapper.eq(RiceFarmingLog::getPlantingBatchId, plantingBatchId)
                .eq(StringUtils.hasText(operationType), RiceFarmingLog::getOperationType, operationType)
                .ge(start != null, RiceFarmingLog::getOperationTime, start)
                .le(end != null, RiceFarmingLog::getOperationTime, end)
                .orderByDesc(RiceFarmingLog::getOperationTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    /** 记录摘要 = 关键字段 JSON 的 SHA-256 */
    private String computeDataHash(RiceFarmingLog logRecord) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(logRecord));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(logRecord.getLogId()));
        }
    }

    /** 根据文件 ID 查询 SHA-256 指纹列表 */
    private List<String> fileHashesOf(List<String> fileIds) {
        List<String> hashes = new ArrayList<>();
        if (fileIds == null) {
            return hashes;
        }
        for (String fileId : fileIds) {
            FileResource resource = fileResourceMapper.selectById(fileId);
            if (resource != null) {
                hashes.add(resource.getSha256());
            }
        }
        return hashes;
    }
}
