package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.MillingBatchCompleteDTO;
import com.itheima.qukuailian.dto.MillingBatchCreateDTO;
import com.itheima.qukuailian.entity.*;
import com.itheima.qukuailian.mapper.*;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceMillingBatchService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceMillingBatchServiceImpl extends ServiceImpl<RiceMillingBatchMapper, RiceMillingBatch>
        implements RiceMillingBatchService {

    /** 精米产出率合理区间（%），超出即生成预警 */
    private static final BigDecimal YIELD_MIN = new BigDecimal("55");
    private static final BigDecimal YIELD_MAX = new BigDecimal("75");

    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RiceQualityTestMapper qualityTestMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceChainProofMapper chainProofMapper;
    private final RiceRiskWarningMapper riskWarningMapper;
    private final FileResourceMapper fileResourceMapper;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceMillingBatch create(MillingBatchCreateDTO dto, String ip) {
        // 1. 原粮批次必须存在（入库单）
        RiceStorageReceipt receipt = storageReceiptMapper.selectOne(
                new LambdaQueryWrapper<RiceStorageReceipt>()
                        .eq(RiceStorageReceipt::getGrainBatchId, dto.getGrainBatchId()));
        if (receipt == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "原粮批次不存在: " + dto.getGrainBatchId());
        }
        // 2. 入库质检必须合格（接口文档 §5.18）
        RiceQualityTest qualityTest = qualityTestMapper.selectOne(
                new LambdaQueryWrapper<RiceQualityTest>()
                        .eq(RiceQualityTest::getBusinessType, "STORAGE")
                        .eq(RiceQualityTest::getBusinessId, receipt.getStorageReceiptId())
                        .orderByDesc(RiceQualityTest::getCreateTime)
                        .last("LIMIT 1"));
        if (qualityTest == null || !"PASS".equals(qualityTest.getOverallResult())) {
            throw new BizException(ResultCode.VALIDATION_FAILED.getCode(),
                    "原粮批次 " + dto.getGrainBatchId() + " 入库质检不合格，不允许加工");
        }
        // 3. 成品米批次必须存在
        RiceProductBatch productBatch = productBatchMapper.selectById(dto.getProductBatchId());
        if (productBatch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "成品米批次不存在: " + dto.getProductBatchId());
        }

        RiceMillingBatch batch = new RiceMillingBatch();
        BeanUtils.copyProperties(dto, batch);
        batch.setMillingBatchId(IdGen.generate("MB"));
        batch.setChainStatus("PENDING");
        save(batch);

        // 4. 原粮批次与成品批次映射关系上链
        chainProofService.submit("MILLING_BATCH", batch.getMillingBatchId(), digest(batch), null);
        auditLogService.record("MILLING_BATCH", batch.getMillingBatchId(), "CREATE",
                null, digest(batch), "创建加工批次", ip);
        log.info("加工批次创建成功: millingBatchId={}, grain={}, product={}",
                batch.getMillingBatchId(), dto.getGrainBatchId(), dto.getProductBatchId());
        return batch;
    }

    @Override
    public IPage<RiceMillingBatch> page(long pageNo, long pageSize, String grainBatchId, String productBatchId,
                                        String factoryId, String startTime, String endTime) {
        LambdaQueryWrapper<RiceMillingBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(grainBatchId), RiceMillingBatch::getGrainBatchId, grainBatchId)
                .eq(StringUtils.hasText(productBatchId), RiceMillingBatch::getProductBatchId, productBatchId)
                .eq(StringUtils.hasText(factoryId), RiceMillingBatch::getFactoryId, factoryId)
                .ge(StringUtils.hasText(startTime), RiceMillingBatch::getProcessStartTime, (org.springframework.util.StringUtils.hasText(startTime) ? java.time.LocalDateTime.parse(startTime) : null))
                .le(StringUtils.hasText(endTime), RiceMillingBatch::getProcessStartTime, (org.springframework.util.StringUtils.hasText(endTime) ? java.time.LocalDateTime.parse(endTime) : null))
                .orderByDesc(RiceMillingBatch::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public RiceMillingBatch detail(String millingBatchId) {
        RiceMillingBatch batch = getById(millingBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "加工批次不存在: " + millingBatchId);
        }
        batch.setProductBatch(productBatchMapper.selectById(batch.getProductBatchId()));
        batch.setChainProof(chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                .eq(RiceChainProof::getBusinessType, "MILLING_BATCH")
                .eq(RiceChainProof::getBusinessId, millingBatchId)));
        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceMillingBatch complete(String millingBatchId, MillingBatchCompleteDTO dto, String ip) {
        RiceMillingBatch batch = getById(millingBatchId);
        if (batch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "加工批次不存在: " + millingBatchId);
        }
        if (batch.getRiceOutputWeightKg() != null) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "加工批次已完成，请勿重复操作");
        }

        // 1. 计算精米产出率 = 精米产出量 / 稻谷出库量 * 100
        BigDecimal yieldRate = dto.getRiceOutputWeightKg()
                .divide(batch.getGrainOutWeightKg(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        batch.setRiceOutputWeightKg(dto.getRiceOutputWeightKg());
        batch.setProcessEndTime(dto.getProcessEndTime());
        batch.setProcessParams(dto.getProcessParams());
        batch.setQualitySummary(dto.getQualitySummary());
        batch.setQualityReportFileId(dto.getQualityReportFileId());
        batch.setYieldRate(yieldRate);
        updateById(batch);

        // 2. 出厂质检摘要 + 报告哈希上链
        String reportHash = fileHashOf(dto.getQualityReportFileId());
        List<String> fileHashes = reportHash == null ? null : List.of(reportHash);
        chainProofService.submit("MILLING_BATCH", millingBatchId, digest(batch), fileHashes);

        // 3. 产出率超出阈值 -> 生成监管预警（接口文档 §5.21）
        if (yieldRate.compareTo(YIELD_MIN) < 0 || yieldRate.compareTo(YIELD_MAX) > 0) {
            createYieldWarning(batch, yieldRate);
        }

        // 4. 成品批次状态 -> PACKAGED
        RiceProductBatch productBatch = productBatchMapper.selectById(batch.getProductBatchId());
        if (productBatch != null && !"ON_SALE".equals(productBatch.getStatus())) {
            productBatch.setStatus("PACKAGED");
            productBatchMapper.updateById(productBatch);
        }

        auditLogService.record("MILLING_BATCH", millingBatchId, "UPDATE",
                null, digest(batch), StringUtils.hasText(dto.getReason()) ? dto.getReason() : "完成加工", ip);
        log.info("加工完成: millingBatchId={}, yieldRate={}%", millingBatchId, yieldRate);
        return batch;
    }

    /** 产出率异常预警 */
    private void createYieldWarning(RiceMillingBatch batch, BigDecimal yieldRate) {
        BigDecimal deviation = yieldRate.compareTo(YIELD_MIN) < 0
                ? YIELD_MIN.subtract(yieldRate) : yieldRate.subtract(YIELD_MAX);
        String riskLevel = deviation.compareTo(new BigDecimal("10")) > 0 ? "HIGH"
                : deviation.compareTo(new BigDecimal("5")) > 0 ? "MEDIUM" : "LOW";

        RiceRiskWarning warning = new RiceRiskWarning();
        warning.setWarningId(IdGen.generate("WARN"));
        warning.setWarningType("YIELD_BALANCE");
        warning.setRiskLevel(riskLevel);
        warning.setBusinessType("MILLING_BATCH");
        warning.setBusinessId(batch.getMillingBatchId());
        warning.setWarningContent(String.format("加工批次 %s 精米产出率 %s%% 超出合理区间 %s%%-%s%%",
                batch.getMillingBatchId(), yieldRate, YIELD_MIN, YIELD_MAX));
        warning.setHandled(false);
        warning.setChainStatus("PENDING");
        riskWarningMapper.insert(warning);
        log.warn("产出率异常预警生成: warningId={}, yieldRate={}%", warning.getWarningId(), yieldRate);
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
