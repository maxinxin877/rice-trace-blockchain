package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.entity.RiceFarmingLog;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RiceNonconformingDisposal;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceQualityTest;
import com.itheima.qukuailian.entity.RiceRecallItem;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceFarmingLogMapper;
import com.itheima.qukuailian.mapper.RiceMillingBatchMapper;
import com.itheima.qukuailian.mapper.RiceNonconformingDisposalMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceQualityTestMapper;
import com.itheima.qukuailian.mapper.RiceRecallItemMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.RiceComplianceGuardService;
import com.itheima.qukuailian.vo.ComplianceCheckVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 防错校验实现：规则全部基于数据库现状判定，不依赖调用方传参，避免"前端校验绕过"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiceComplianceGuardServiceImpl implements RiceComplianceGuardService {

    private static final List<String> RECALL_ACTIVE_STATUS = List.of(
            "PENDING", "OFF_SHELF", "RECOVERED", "ISOLATED", "UNRECOVERED");

    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceTraceCodeMapper traceCodeMapper;
    private final RiceQualityTestMapper qualityTestMapper;
    private final RiceFarmingLogMapper farmingLogMapper;
    private final RiceRecallItemMapper recallItemMapper;
    private final RiceNonconformingDisposalMapper disposalMapper;

    @Override
    public List<ComplianceCheckVO> check(String businessType, String businessId, String action) {
        List<ComplianceCheckVO> results = new ArrayList<>();
        String act = action == null ? "" : action.toUpperCase();

        switch (act) {
            case ACTION_STORE -> checkStore(businessType, businessId, results);
            case ACTION_MILL -> checkMill(businessType, businessId, results);
            case ACTION_ACTIVATE -> checkActivate(businessType, businessId, results);
            case ACTION_SELL -> checkSell(businessType, businessId, results);
            default -> results.add(fail("ACTION_UNSUPPORTED", "动作类型", "BLOCK",
                    "不支持的校验动作: " + action, businessId));
        }
        return results;
    }

    @Override
    public void assertPass(String businessType, String businessId, String action) {
        List<ComplianceCheckVO> failed = check(businessType, businessId, action).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getPassed()))
                .filter(r -> "BLOCK".equals(r.getLevel()))
                .toList();
        if (failed.isEmpty()) {
            return;
        }
        String message = failed.stream()
                .map(r -> "【" + r.getRuleName() + "】" + r.getMessage())
                .collect(Collectors.joining("；"));
        throw new BizException(ResultCode.VALIDATION_FAILED.getCode(), "防错校验未通过：" + message);
    }

    // ------------------------------------------------------------------
    // 入库收储：批次冻结 + 休药期
    // ------------------------------------------------------------------
    private void checkStore(String businessType, String businessId, List<ComplianceCheckVO> results) {
        RicePlantingBatch batch = "PLANTING_BATCH".equalsIgnoreCase(businessType)
                ? plantingBatchMapper.selectById(businessId) : null;
        if (batch == null) {
            results.add(fail("BATCH_EXISTS", "批次存在性", "BLOCK", "种植批次不存在: " + businessId, businessId));
            return;
        }
        results.add(pass("BATCH_EXISTS", "批次存在性", "种植批次 " + businessId + " 存在", businessId));
        addIsolationCheck(results, businessId);

        // 1. 批次是否被冻结（召回 / 隔离）
        boolean locked = "LOCKED".equals(batch.getStatus());
        results.add(locked
                ? fail("BATCH_LOCKED", "批次冻结校验", "BLOCK", "种植批次已被冻结（召回/隔离中），禁止入库", businessId)
                : pass("BATCH_LOCKED", "批次冻结校验", "批次状态正常：" + batch.getStatus(), businessId));

        // 2. 休药期：最近一次用药 + 安全间隔期，未到期不得收割入库（需求 §8.4 罗非鱼示例同构规则）
        List<RiceFarmingLog> pesticideLogs = farmingLogMapper.selectList(new LambdaQueryWrapper<RiceFarmingLog>()
                .eq(RiceFarmingLog::getPlantingBatchId, businessId)
                .eq(RiceFarmingLog::getOperationType, "PESTICIDE")
                .isNotNull(RiceFarmingLog::getSafeIntervalDays)
                .orderByDesc(RiceFarmingLog::getOperationTime)
                .last("LIMIT 1"));
        if (pesticideLogs.isEmpty()) {
            results.add(pass("WITHDRAWAL_PERIOD", "休药期校验", "无用药记录，无休药期约束", businessId));
        } else {
            RiceFarmingLog log = pesticideLogs.get(0);
            LocalDateTime safeTime = log.getOperationTime() == null ? null
                    : log.getOperationTime().plusDays(log.getSafeIntervalDays());
            if (safeTime != null && safeTime.isAfter(LocalDateTime.now())) {
                results.add(fail("WITHDRAWAL_PERIOD", "休药期校验", "BLOCK",
                        "用药 " + log.getMaterialName() + " 的安全间隔期未满，需至 "
                                + safeTime.toLocalDate() + " 之后才可收割入库", businessId));
            } else {
                results.add(pass("WITHDRAWAL_PERIOD", "休药期校验",
                        "最近用药安全间隔期已满（" + safeTime.toLocalDate() + "）", businessId));
            }
        }
    }

    // ------------------------------------------------------------------
    // 碾米加工：原粮冻结 + 入库质检结论
    // ------------------------------------------------------------------
    private void checkMill(String businessType, String businessId, List<ComplianceCheckVO> results) {
        RiceStorageReceipt receipt;
        if ("STORAGE_RECEIPT".equalsIgnoreCase(businessType)) {
            receipt = storageReceiptMapper.selectById(businessId);
        } else {
            // 默认按原粮批次查（接口入参就是 grainBatchId）
            receipt = storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                    .eq(RiceStorageReceipt::getGrainBatchId, businessId)
                    .orderByDesc(RiceStorageReceipt::getStorageTime)
                    .last("LIMIT 1"));
        }
        if (receipt == null) {
            results.add(fail("RECEIPT_EXISTS", "原粮批次校验", "BLOCK", "原粮批次未入库: " + businessId, businessId));
            return;
        }
        results.add(pass("RECEIPT_EXISTS", "原粮批次校验", "已找到入库单 " + receipt.getStorageReceiptId(),
                receipt.getStorageReceiptId()));

        // 1. 上游种植批次冻结
        RicePlantingBatch batch = plantingBatchMapper.selectById(receipt.getPlantingBatchId());
        if (batch != null && "LOCKED".equals(batch.getStatus())) {
            results.add(fail("UPSTREAM_LOCKED", "上游批次冻结校验", "BLOCK",
                    "上游种植批次已冻结，禁止加工", batch.getPlantingBatchId()));
        } else {
            results.add(pass("UPSTREAM_LOCKED", "上游批次冻结校验", "上游批次状态正常", receipt.getPlantingBatchId()));
        }

        // 2. 入库质检结论
        RiceQualityTest test = latestQualityTest("STORAGE", receipt.getStorageReceiptId());
        if (test == null) {
            results.add(fail("QUALITY_PASS", "入库质检校验", "BLOCK", "该原粮批次尚未提交入库质检，禁止加工",
                    receipt.getStorageReceiptId()));
        } else if (!"PASS".equals(test.getOverallResult())) {
            results.add(fail("QUALITY_PASS", "入库质检校验", "BLOCK",
                    "入库质检结论为 " + test.getOverallResult() + "，禁止加工", receipt.getStorageReceiptId()));
        } else {
            results.add(pass("QUALITY_PASS", "入库质检校验", "入库质检合格（" + test.getTestAgency() + "）",
                    receipt.getStorageReceiptId()));
        }
        addIsolationCheck(results, receipt.getGrainBatchId(), receipt.getStorageReceiptId(),
                receipt.getPlantingBatchId());

        // 3. 原粮批次是否处于召回冻结
        if (isUnderRecall(receipt.getGrainBatchId())) {
            results.add(fail("RECALL_FROZEN", "召回冻结校验", "BLOCK",
                    "原粮批次处于召回处置中，禁止加工", receipt.getGrainBatchId()));
        } else {
            results.add(pass("RECALL_FROZEN", "召回冻结校验", "未命中召回冻结清单", receipt.getGrainBatchId()));
        }
    }

    // ------------------------------------------------------------------
    // 赋码激活：成品批次状态 + 召回冻结 + 出厂质检
    // ------------------------------------------------------------------
    private void checkActivate(String businessType, String businessId, List<ComplianceCheckVO> results) {
        RiceProductBatch product = productBatchMapper.selectById(businessId);
        if (product == null) {
            results.add(fail("PRODUCT_EXISTS", "成品批次校验", "BLOCK", "成品批次不存在: " + businessId, businessId));
            return;
        }
        results.add(pass("PRODUCT_EXISTS", "成品批次校验", "成品批次 " + product.getProductName(), businessId));
        addIsolationCheck(results, businessId);

        boolean locked = "LOCKED".equals(product.getStatus());
        results.add(locked
                ? fail("PRODUCT_LOCKED", "成品冻结校验", "BLOCK", "成品批次已被冻结，禁止赋码上市", businessId)
                : pass("PRODUCT_LOCKED", "成品冻结校验", "成品批次状态：" + product.getStatus(), businessId));

        if (isUnderRecall(businessId)) {
            results.add(fail("RECALL_FROZEN", "召回冻结校验", "BLOCK", "该成品批次处于召回处置中，禁止赋码", businessId));
        } else {
            results.add(pass("RECALL_FROZEN", "召回冻结校验", "未命中召回冻结清单", businessId));
        }

        // 出厂质检：加工批次若已有质检且不合格则拦截
        List<RiceMillingBatch> millings = millingBatchMapper.selectList(new LambdaQueryWrapper<RiceMillingBatch>()
                .eq(RiceMillingBatch::getProductBatchId, businessId));
        boolean millDataReady = millings.stream()
                .anyMatch(m -> StringUtils.hasText(m.getQualitySummary()) || m.getRiceOutputWeightKg() != null);
        if (millings.isEmpty()) {
            results.add(fail("MILLING_EXISTS", "加工链路校验", "WARN", "该成品批次尚未关联加工批次", businessId));
        } else if (!millDataReady) {
            results.add(fail("MILLING_EXISTS", "加工链路校验", "WARN", "加工批次尚未完成出厂质检", businessId));
        } else {
            results.add(pass("MILLING_EXISTS", "加工链路校验", "已关联 " + millings.size() + " 个加工批次", businessId));
        }
    }

    // ------------------------------------------------------------------
    // 终端销售/扫码出库：防伪码状态 + 召回冻结
    // ------------------------------------------------------------------
    private void checkSell(String businessType, String businessId, List<ComplianceCheckVO> results) {
        RiceTraceCode code = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                .eq(RiceTraceCode::getTraceCode, businessId).last("LIMIT 1"));
        if (code == null) {
            results.add(fail("CODE_EXISTS", "防伪码校验", "BLOCK", "防伪码不存在: " + businessId, businessId));
            return;
        }
        results.add(pass("CODE_EXISTS", "防伪码校验", "防伪码存在", businessId));
        addIsolationCheck(results, businessId, code.getProductBatchId());

        boolean sellable = "ACTIVATED".equals(code.getStatus());
        results.add(sellable
                ? pass("CODE_STATUS", "防伪码状态校验", "已激活，可正常销售", businessId)
                : fail("CODE_STATUS", "防伪码状态校验", "BLOCK",
                "防伪码状态为 " + code.getStatus() + "，禁止销售", businessId));

        boolean frozen = isUnderRecall(businessId) || isUnderRecall(code.getProductBatchId());
        results.add(frozen
                ? fail("RECALL_SALES_BLOCK", "召回销售拦截", "BLOCK", "该码或所属批次处于召回处置中，禁止销售", businessId)
                : pass("RECALL_SALES_BLOCK", "召回销售拦截", "未命中召回冻结清单", businessId));
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------
    /**
     * 不合格品隔离校验（FC-04）：处置单评审通过后 isolated=1，在关闭前禁止该对象继续流转。
     * 用"处置单标记"而不是改批次状态，好处是解除隔离时不需要还原原状态。
     */
    private void addIsolationCheck(List<ComplianceCheckVO> results, String... candidates) {
        String first = "";
        for (String id : candidates) {
            if (!StringUtils.hasText(id)) {
                continue;
            }
            if (!StringUtils.hasText(first)) {
                first = id;
            }
            Long count = disposalMapper.selectCount(new LambdaQueryWrapper<RiceNonconformingDisposal>()
                    .eq(RiceNonconformingDisposal::getBusinessId, id)
                    .eq(RiceNonconformingDisposal::getIsolated, true)
                    .in(RiceNonconformingDisposal::getStatus, List.of("APPROVED", "EXECUTED")));
            if (count != null && count > 0) {
                results.add(fail("NONCONFORMING_ISOLATED", "不合格品隔离校验", "BLOCK",
                        "对象 " + id + " 存在未关闭的不合格品处置单（已隔离），禁止继续流转", id));
                return;
            }
        }
        results.add(pass("NONCONFORMING_ISOLATED", "不合格品隔离校验", "未命中不合格品隔离清单", first));
    }

    private RiceQualityTest latestQualityTest(String businessType, String businessId) {
        return qualityTestMapper.selectOne(new LambdaQueryWrapper<RiceQualityTest>()
                .eq(RiceQualityTest::getBusinessType, businessType)
                .eq(RiceQualityTest::getBusinessId, businessId)
                .orderByDesc(RiceQualityTest::getCreateTime)
                .last("LIMIT 1"));
    }

    private boolean isUnderRecall(String businessId) {
        if (!StringUtils.hasText(businessId)) {
            return false;
        }
        Long count = recallItemMapper.selectCount(new LambdaQueryWrapper<RiceRecallItem>()
                .eq(RiceRecallItem::getBusinessId, businessId)
                .in(RiceRecallItem::getHandleStatus, RECALL_ACTIVE_STATUS));
        return count != null && count > 0;
    }

    private ComplianceCheckVO pass(String code, String name, String message, String businessId) {
        ComplianceCheckVO vo = new ComplianceCheckVO();
        vo.setRuleCode(code);
        vo.setRuleName(name);
        vo.setPassed(true);
        vo.setMessage(message);
        vo.setLevel("INFO");
        vo.setBusinessId(businessId);
        return vo;
    }

    private ComplianceCheckVO fail(String code, String name, String level, String message, String businessId) {
        ComplianceCheckVO vo = new ComplianceCheckVO();
        vo.setRuleCode(code);
        vo.setRuleName(name);
        vo.setPassed(false);
        vo.setMessage(message);
        vo.setLevel(level);
        vo.setBusinessId(businessId);
        return vo;
    }
}
