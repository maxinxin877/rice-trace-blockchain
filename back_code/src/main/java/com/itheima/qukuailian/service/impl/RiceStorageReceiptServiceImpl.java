package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.QualityTestItemDTO;
import com.itheima.qukuailian.dto.QualityTestSubmitDTO;
import com.itheima.qukuailian.dto.StorageReceiptCreateDTO;
import com.itheima.qukuailian.entity.*;
import com.itheima.qukuailian.mapper.*;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceStorageReceiptService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
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
public class RiceStorageReceiptServiceImpl extends ServiceImpl<RiceStorageReceiptMapper, RiceStorageReceipt>
        implements RiceStorageReceiptService {

    private static final List<String> STORED_STATUS = List.of("STORED", "MILLING", "PACKAGED", "ON_SALE", "LOCKED");

    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceFieldMapper fieldMapper;
    private final RiceQualityTestMapper qualityTestMapper;
    private final RiceQualityTestItemMapper qualityTestItemMapper;
    private final RiceChainProofMapper chainProofMapper;
    private final FileResourceMapper fileResourceMapper;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceStorageReceipt create(StorageReceiptCreateDTO dto, String ip) {
        // 1. 种植批次存在且未入库
        RicePlantingBatch batch = plantingBatchMapper.selectById(dto.getPlantingBatchId());
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "种植批次不存在: " + dto.getPlantingBatchId());
        }
        if (STORED_STATUS.contains(batch.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "该种植批次已入库，不能重复入库");
        }
        // 2. 原粮批次唯一
        long count = lambdaQuery().eq(RiceStorageReceipt::getGrainBatchId, dto.getGrainBatchId()).count();
        if (count > 0) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "原粮批次已存在入库单: " + dto.getGrainBatchId());
        }

        // 3. 创建入库单
        RiceStorageReceipt receipt = new RiceStorageReceipt();
        BeanUtils.copyProperties(dto, receipt);
        receipt.setStorageReceiptId(IdGen.generate("SREC"));
        receipt.setChainStatus("PENDING");
        save(receipt);

        // 4. 种植批次状态 -> STORED
        batch.setStatus("STORED");
        plantingBatchMapper.updateById(batch);

        // 5. 入库单摘要（含双方电子签名）上链
        chainProofService.submit("STORAGE_RECEIPT", receipt.getStorageReceiptId(), digest(receipt), null);
        auditLogService.record("STORAGE_RECEIPT", receipt.getStorageReceiptId(), "CREATE",
                null, digest(receipt), "创建入库单", ip);
        log.info("入库单创建成功: storageReceiptId={}, grainBatchId={}", receipt.getStorageReceiptId(), dto.getGrainBatchId());
        return receipt;
    }

    @Override
    public IPage<RiceStorageReceipt> page(long pageNo, long pageSize, String grainBatchId, String plantingBatchId,
                                          String warehouseId, String grainGrade, String startTime, String endTime) {
        LambdaQueryWrapper<RiceStorageReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(grainBatchId), RiceStorageReceipt::getGrainBatchId, grainBatchId)
                .eq(StringUtils.hasText(plantingBatchId), RiceStorageReceipt::getPlantingBatchId, plantingBatchId)
                .eq(StringUtils.hasText(warehouseId), RiceStorageReceipt::getWarehouseId, warehouseId)
                .eq(StringUtils.hasText(grainGrade), RiceStorageReceipt::getGrainGrade, grainGrade)
                .ge(StringUtils.hasText(startTime), RiceStorageReceipt::getStorageTime, (org.springframework.util.StringUtils.hasText(startTime) ? java.time.LocalDateTime.parse(startTime) : null))
                .le(StringUtils.hasText(endTime), RiceStorageReceipt::getStorageTime, (org.springframework.util.StringUtils.hasText(endTime) ? java.time.LocalDateTime.parse(endTime) : null))
                .orderByDesc(RiceStorageReceipt::getStorageTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public RiceStorageReceipt detail(String storageReceiptId) {
        RiceStorageReceipt receipt = getById(storageReceiptId);
        if (receipt == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "入库单不存在: " + storageReceiptId);
        }
        // 种植批次 + 地块摘要
        RicePlantingBatch batch = plantingBatchMapper.selectById(receipt.getPlantingBatchId());
        receipt.setPlantingBatch(batch);
        if (batch != null) {
            receipt.setField(fieldMapper.selectById(batch.getFieldId()));
        }
        // 入库质检（最近一次）
        receipt.setQualityTest(latestQualityTest("STORAGE", storageReceiptId));
        // 链上存证
        receipt.setChainProof(chainProofOf("STORAGE_RECEIPT", storageReceiptId));
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceQualityTest submitQualityTest(String storageReceiptId, QualityTestSubmitDTO dto) {
        RiceStorageReceipt receipt = getById(storageReceiptId);
        if (receipt == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "入库单不存在: " + storageReceiptId);
        }

        // 1. 报告哈希 = 报告文件的 SHA-256
        String reportHash = fileHashOf(dto.getReportFileId());

        // 2. 质检记录 + 明细
        RiceQualityTest test = new RiceQualityTest();
        test.setQualityTestId(IdGen.generate("QT"));
        test.setBusinessType("STORAGE");
        test.setBusinessId(storageReceiptId);
        test.setTestAgency(dto.getTestAgency());
        test.setTestTime(dto.getTestTime());
        test.setOverallResult(dto.getOverallResult());
        test.setReportFileId(dto.getReportFileId());
        test.setReportHash(reportHash);
        test.setRemark(dto.getRemark());
        test.setChainStatus("PENDING");
        qualityTestMapper.insert(test);

        List<RiceQualityTestItem> items = new ArrayList<>();
        for (QualityTestItemDTO itemDto : dto.getTestItems()) {
            RiceQualityTestItem item = new RiceQualityTestItem();
            BeanUtils.copyProperties(itemDto, item);
            // DTO 字段名为 value，实体字段名为 itemValue，需显式映射
            item.setItemValue(itemDto.getValue());
            item.setQualityTestId(test.getQualityTestId());
            items.add(item);
            qualityTestItemMapper.insert(item);
        }
        test.setItems(items);

        // 3. 回写入库单报告信息
        receipt.setQualityReportFileId(dto.getReportFileId());
        receipt.setReportHash(reportHash);
        updateById(receipt);

        // 4. 质检摘要 + 报告哈希上链
        List<String> fileHashes = reportHash == null ? null : List.of(reportHash);
        chainProofService.submit("QUALITY_TEST", test.getQualityTestId(), digest(test), fileHashes);
        log.info("入库质检提交成功: qualityTestId={}, result={}", test.getQualityTestId(), dto.getOverallResult());
        return test;
    }

    /** 最近一次质检记录（含明细） */
    private RiceQualityTest latestQualityTest(String businessType, String businessId) {
        RiceQualityTest test = qualityTestMapper.selectOne(
                new LambdaQueryWrapper<RiceQualityTest>()
                        .eq(RiceQualityTest::getBusinessType, businessType)
                        .eq(RiceQualityTest::getBusinessId, businessId)
                        .orderByDesc(RiceQualityTest::getCreateTime)
                        .last("LIMIT 1"));
        if (test == null) {
            return null;
        }
        test.setItems(qualityTestItemMapper.selectList(
                new LambdaQueryWrapper<RiceQualityTestItem>()
                        .eq(RiceQualityTestItem::getQualityTestId, test.getQualityTestId())));
        return test;
    }

    private RiceChainProof chainProofOf(String businessType, String businessId) {
        return chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                .eq(RiceChainProof::getBusinessType, businessType)
                .eq(RiceChainProof::getBusinessId, businessId));
    }

    private String fileHashOf(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return null;
        }
        FileResource resource = fileResourceMapper.selectById(fileId);
        return resource == null ? null : resource.getSha256();
    }

    private String digest(Object obj) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(obj));
        }
    }
}
