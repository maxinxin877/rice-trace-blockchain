package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceField;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceFieldMapper;
import com.itheima.qukuailian.mapper.RiceMillingBatchMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.RiceProductBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceProductBatchServiceImpl extends ServiceImpl<RiceProductBatchMapper, RiceProductBatch>
        implements RiceProductBatchService {

    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceFieldMapper fieldMapper;
    private final RiceTraceCodeMapper traceCodeMapper;

    @Override
    public RiceProductBatch create(ProductBatchCreateDTO dto) {
        // productBatchId 业务唯一
        long count = lambdaQuery().eq(RiceProductBatch::getProductBatchId, dto.getProductBatchId()).count();
        if (count > 0) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "成品米批次已存在: " + dto.getProductBatchId());
        }
        // 成品批次ID必须已在碾米加工中创建
        Long millingCount = millingBatchMapper.selectCount(
                new LambdaQueryWrapper<RiceMillingBatch>()
                        .eq(RiceMillingBatch::getProductBatchId, dto.getProductBatchId()));
        if (millingCount == null || millingCount == 0) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(),
                    "成品批次ID需先在碾米加工中创建: " + dto.getProductBatchId());
        }
        RiceProductBatch batch = new RiceProductBatch();
        BeanUtils.copyProperties(dto, batch);
        batch.setStatus("DRAFT");
        save(batch);
        log.info("成品米批次创建成功: productBatchId={}, productName={}", batch.getProductBatchId(), dto.getProductName());
        return batch;
    }

    @Override
    public IPage<RiceProductBatch> page(long pageNo, long pageSize, String productBatchId, String productName,
                                        String brandName, String status) {
        LambdaQueryWrapper<RiceProductBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(productBatchId), RiceProductBatch::getProductBatchId, productBatchId)
                .like(StringUtils.hasText(productName), RiceProductBatch::getProductName, productName)
                .eq(StringUtils.hasText(brandName), RiceProductBatch::getBrandName, brandName)
                .eq(StringUtils.hasText(status), RiceProductBatch::getStatus, status)
                .orderByDesc(RiceProductBatch::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public Map<String, Object> trace(String productBatchId) {
        RiceProductBatch productBatch = getById(productBatchId);
        if (productBatch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "成品米批次不存在: " + productBatchId);
        }
        // 碾米加工（成品批次 → 加工批次）
        RiceMillingBatch millingBatch = millingBatchMapper.selectOne(
                new LambdaQueryWrapper<RiceMillingBatch>()
                        .eq(RiceMillingBatch::getProductBatchId, productBatchId)
                        .orderByDesc(RiceMillingBatch::getCreateTime)
                        .last("LIMIT 1"));
        // 收储入库（加工批次 → 原粮批次 → 入库单）
        RiceStorageReceipt storageReceipt = null;
        if (millingBatch != null) {
            storageReceipt = storageReceiptMapper.selectOne(
                    new LambdaQueryWrapper<RiceStorageReceipt>()
                            .eq(RiceStorageReceipt::getGrainBatchId, millingBatch.getGrainBatchId())
                            .orderByDesc(RiceStorageReceipt::getCreateTime)
                            .last("LIMIT 1"));
        }
        // 种植批次（入库单 → 种植批次）
        RicePlantingBatch plantingBatch = null;
        if (storageReceipt != null) {
            plantingBatch = plantingBatchMapper.selectById(storageReceipt.getPlantingBatchId());
        }
        // 地块（种植批次 → 地块）
        RiceField field = null;
        if (plantingBatch != null) {
            field = fieldMapper.selectById(plantingBatch.getFieldId());
        }
        // 防伪码（成品批次 → 防伪码）
        List<RiceTraceCode> traceCodes = traceCodeMapper.selectList(
                new LambdaQueryWrapper<RiceTraceCode>()
                        .eq(RiceTraceCode::getProductBatchId, productBatchId));

        Map<String, Object> result = new HashMap<>();
        result.put("productBatch", productBatch);
        result.put("millingBatch", millingBatch);
        result.put("storageReceipt", storageReceipt);
        result.put("plantingBatch", plantingBatch);
        result.put("field", field);
        result.put("traceCodes", traceCodes);
        return result;
    }
}
