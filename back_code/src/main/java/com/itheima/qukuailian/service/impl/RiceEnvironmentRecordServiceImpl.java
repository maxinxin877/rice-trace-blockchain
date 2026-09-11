package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.EnvironmentRecordCreateDTO;
import com.itheima.qukuailian.entity.RiceEnvironmentRecord;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.mapper.RiceEnvironmentRecordMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.service.RiceEnvironmentRecordService;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceEnvironmentRecordServiceImpl extends ServiceImpl<RiceEnvironmentRecordMapper, RiceEnvironmentRecord>
        implements RiceEnvironmentRecordService {

    private final RicePlantingBatchMapper plantingBatchMapper;

    @Override
    public RiceEnvironmentRecord create(String plantingBatchId, EnvironmentRecordCreateDTO dto) {
        RicePlantingBatch batch = plantingBatchMapper.selectById(plantingBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "种植批次不存在: " + plantingBatchId);
        }
        RiceEnvironmentRecord record = new RiceEnvironmentRecord();
        BeanUtils.copyProperties(dto, record);
        record.setEnvironmentRecordId(IdGen.generate("ENV"));
        record.setPlantingBatchId(plantingBatchId);
        save(record);
        log.info("环境数据上传成功: envRecordId={}, batch={}, source={}",
                record.getEnvironmentRecordId(), plantingBatchId, dto.getSourceType());
        return record;
    }

    @Override
    public IPage<RiceEnvironmentRecord> page(String plantingBatchId, String sourceType,
                                             String startTime, String endTime, long pageNo, long pageSize) {
        LambdaQueryWrapper<RiceEnvironmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiceEnvironmentRecord::getPlantingBatchId, plantingBatchId)
                .eq(StringUtils.hasText(sourceType), RiceEnvironmentRecord::getSourceType, sourceType)
                .ge(StringUtils.hasText(startTime), RiceEnvironmentRecord::getRecordTime, (org.springframework.util.StringUtils.hasText(startTime) ? java.time.LocalDateTime.parse(startTime) : null))
                .le(StringUtils.hasText(endTime), RiceEnvironmentRecord::getRecordTime, (org.springframework.util.StringUtils.hasText(endTime) ? java.time.LocalDateTime.parse(endTime) : null))
                .orderByDesc(RiceEnvironmentRecord::getRecordTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }
}
