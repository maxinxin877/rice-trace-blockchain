package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceScanLog;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceFieldMapper;
import com.itheima.qukuailian.mapper.RiceMillingBatchMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceScanLogMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.RiceProductBatchService;
import com.itheima.qukuailian.vo.ProductTraceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceProductBatchServiceImpl extends ServiceImpl<RiceProductBatchMapper, RiceProductBatch>
        implements RiceProductBatchService {

    /** 溯源链路返回的防伪码数量上限 */
    private static final int TRACE_CODE_LIMIT = 50;

    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceFieldMapper fieldMapper;
    private final RiceTraceCodeMapper traceCodeMapper;
    private final RiceScanLogMapper scanLogMapper;
    private final AuditLogService auditLogService;

    @Override
    public RiceProductBatch create(ProductBatchCreateDTO dto) {
        // productBatchId 业务唯一
        long count = lambdaQuery().eq(RiceProductBatch::getProductBatchId, dto.getProductBatchId()).count();
        if (count > 0) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "成品米批次已存在: " + dto.getProductBatchId());
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
                                        String brandName, String status, String riceVariety) {
        LambdaQueryWrapper<RiceProductBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(productBatchId), RiceProductBatch::getProductBatchId, productBatchId)
                .like(StringUtils.hasText(productName), RiceProductBatch::getProductName, productName)
                .eq(StringUtils.hasText(brandName), RiceProductBatch::getBrandName, brandName)
                .eq(StringUtils.hasText(status), RiceProductBatch::getStatus, status)
                .eq(StringUtils.hasText(riceVariety), RiceProductBatch::getRiceVariety, riceVariety)
                .orderByDesc(RiceProductBatch::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public RiceProductBatch detail(String productBatchId) {
        RiceProductBatch batch = getById(productBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "成品米批次不存在: " + productBatchId);
        }
        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceProductBatch update(String productBatchId, ProductBatchCreateDTO dto, String reason, String ip) {
        RiceProductBatch batch = getById(productBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "成品米批次不存在: " + productBatchId);
        }
        String status = batch.getStatus();
        BeanUtils.copyProperties(dto, batch);
        batch.setProductBatchId(productBatchId);
        batch.setStatus(status); // 状态由业务流转控制，不允许通过编辑接口修改
        updateById(batch);
        auditLogService.record("PRODUCT_BATCH", productBatchId, "UPDATE", null, null,
                StringUtils.hasText(reason) ? reason : "编辑成品批次", ip);
        log.info("成品米批次更新成功: productBatchId={}", productBatchId);
        return batch;
    }

    @Override
    public ProductTraceVO trace(String productBatchId) {
        RiceProductBatch productBatch = getById(productBatchId);
        if (productBatch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "成品米批次不存在: " + productBatchId);
        }
        ProductTraceVO vo = new ProductTraceVO();
        vo.setProductBatch(productBatch);

        // 加工批次（按成品批次反查）
        RiceMillingBatch milling = millingBatchMapper.selectOne(new LambdaQueryWrapper<RiceMillingBatch>()
                .eq(RiceMillingBatch::getProductBatchId, productBatchId)
                .orderByDesc(RiceMillingBatch::getCreateTime)
                .last("LIMIT 1"));
        vo.setMillingBatch(milling);

        // 入库单（按原粮批次反查）→ 种植批次 → 地块
        if (milling != null) {
            RiceStorageReceipt storage = storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                    .eq(RiceStorageReceipt::getGrainBatchId, milling.getGrainBatchId()));
            vo.setStorageReceipt(storage);
            if (storage != null) {
                RicePlantingBatch planting = plantingBatchMapper.selectById(storage.getPlantingBatchId());
                vo.setPlantingBatch(planting);
                if (planting != null) {
                    vo.setField(fieldMapper.selectById(planting.getFieldId()));
                }
            }
        }

        // 该成品批次下的防伪码
        List<RiceTraceCode> codes = traceCodeMapper.selectList(new LambdaQueryWrapper<RiceTraceCode>()
                .eq(RiceTraceCode::getProductBatchId, productBatchId)
                .orderByDesc(RiceTraceCode::getCreateTime)
                .last("LIMIT " + TRACE_CODE_LIMIT));
        vo.setTraceCodes(codes);
        return vo;
    }

    @Override
    public IPage<RiceScanLog> pageScanLogs(String traceCode, long pageNo, long pageSize) {
        return scanLogMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<RiceScanLog>()
                        .eq(RiceScanLog::getTraceCode, traceCode)
                        .orderByDesc(RiceScanLog::getScanTime));
    }
}
