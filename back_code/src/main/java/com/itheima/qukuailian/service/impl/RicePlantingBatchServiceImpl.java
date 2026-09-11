package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.PlantingBatchCreateDTO;
import com.itheima.qukuailian.dto.PlantingBatchUpdateDTO;
import com.itheima.qukuailian.entity.RiceEnvironmentRecord;
import com.itheima.qukuailian.entity.RiceFarmingLog;
import com.itheima.qukuailian.entity.RiceField;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.mapper.RiceEnvironmentRecordMapper;
import com.itheima.qukuailian.mapper.RiceFarmingLogMapper;
import com.itheima.qukuailian.mapper.RiceFieldMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.RicePlantingBatchService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RicePlantingBatchServiceImpl extends ServiceImpl<RicePlantingBatchMapper, RicePlantingBatch>
        implements RicePlantingBatchService {

    /** 未结束的种植批次状态 */
    private static final List<String> UNFINISHED_STATUS = List.of("DRAFT", "PLANTED", "HARVESTED");
    /** 已入库后的状态，禁止修改确地确种信息 */
    private static final List<String> STORED_STATUS = List.of("STORED", "MILLING", "PACKAGED", "ON_SALE", "LOCKED");

    private final RiceFieldMapper fieldMapper;
    private final RiceFarmingLogMapper farmingLogMapper;
    private final RiceEnvironmentRecordMapper environmentRecordMapper;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RicePlantingBatch create(PlantingBatchCreateDTO dto, String ip) {
        // 1. 地块必须存在（确地块）
        RiceField field = fieldMapper.selectById(dto.getFieldId());
        if (field == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "地块不存在: " + dto.getFieldId());
        }
        // 2. 同一地块同一时间只能有一个未结束的种植批次
        long unfinished = lambdaQuery()
                .eq(RicePlantingBatch::getFieldId, dto.getFieldId())
                .in(RicePlantingBatch::getStatus, UNFINISHED_STATUS)
                .count();
        if (unfinished > 0) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "该地块存在未结束的种植批次，请先完成收割或入库");
        }

        // 3. 创建批次（确地块、确种子），状态 PLANTED
        RicePlantingBatch batch = new RicePlantingBatch();
        BeanUtils.copyProperties(dto, batch);
        batch.setPlantingBatchId(IdGen.generate("RPB"));
        batch.setStatus("PLANTED");
        batch.setChainStatus("PENDING");
        save(batch);

        auditLogService.record("PLANTING_BATCH", batch.getPlantingBatchId(), "CREATE",
                null, digest(batch), "创建种植批次", ip);
        log.info("种植批次创建成功: plantingBatchId={}, fieldId={}", batch.getPlantingBatchId(), dto.getFieldId());
        return batch;
    }

    @Override
    public IPage<RicePlantingBatch> page(long pageNo, long pageSize, String fieldId, String riceVariety,
                                         String status, String sowingDateStart, String sowingDateEnd) {
        LambdaQueryWrapper<RicePlantingBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(fieldId), RicePlantingBatch::getFieldId, fieldId)
                .like(StringUtils.hasText(riceVariety), RicePlantingBatch::getRiceVariety, riceVariety)
                .eq(StringUtils.hasText(status), RicePlantingBatch::getStatus, status)
                .ge(StringUtils.hasText(sowingDateStart), RicePlantingBatch::getSowingDate, (org.springframework.util.StringUtils.hasText(sowingDateStart) ? java.time.LocalDate.parse(sowingDateStart) : null))
                .le(StringUtils.hasText(sowingDateEnd), RicePlantingBatch::getSowingDate, (org.springframework.util.StringUtils.hasText(sowingDateEnd) ? java.time.LocalDate.parse(sowingDateEnd) : null))
                .orderByDesc(RicePlantingBatch::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public RicePlantingBatch detail(String plantingBatchId) {
        RicePlantingBatch batch = getById(plantingBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "种植批次不存在: " + plantingBatchId);
        }
        // 地块摘要
        batch.setField(fieldMapper.selectById(batch.getFieldId()));
        // 农事记录 / 环境数据摘要
        batch.setFarmingLogCount(Math.toIntExact(farmingLogMapper.selectCount(
                new LambdaQueryWrapper<RiceFarmingLog>()
                        .eq(RiceFarmingLog::getPlantingBatchId, plantingBatchId))));
        batch.setEnvironmentRecordCount(Math.toIntExact(environmentRecordMapper.selectCount(
                new LambdaQueryWrapper<RiceEnvironmentRecord>()
                        .eq(RiceEnvironmentRecord::getPlantingBatchId, plantingBatchId))));
        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RicePlantingBatch update(String plantingBatchId, PlantingBatchUpdateDTO dto, String ip) {
        RicePlantingBatch batch = getById(plantingBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "种植批次不存在: " + plantingBatchId);
        }
        // 已入库的批次不允许修改 fieldId / riceVariety / seedSource（接口文档 §5.9）
        if (STORED_STATUS.contains(batch.getStatus())) {
            if (!batch.getFieldId().equals(dto.getFieldId())
                    || !batch.getRiceVariety().equals(dto.getRiceVariety())
                    || !batch.getSeedSource().equals(dto.getSeedSource())) {
                throw new BizException(ResultCode.CONFLICT.getCode(), "批次已入库，不允许修改地块/品种/种子来源");
            }
        }
        String beforeDigest = digest(batch);
        BeanUtils.copyProperties(dto, batch);
        batch.setPlantingBatchId(plantingBatchId);
        updateById(batch);

        auditLogService.record("PLANTING_BATCH", plantingBatchId, "UPDATE",
                beforeDigest, digest(batch), dto.getReason(), ip);
        log.info("种植批次更新成功: plantingBatchId={}", plantingBatchId);
        return batch;
    }

    /** 业务摘要：关键字段的 SHA-256 */
    private String digest(RicePlantingBatch batch) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(batch));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(batch.getPlantingBatchId()));
        }
    }
}
