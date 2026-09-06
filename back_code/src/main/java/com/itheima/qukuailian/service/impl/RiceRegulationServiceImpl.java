package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.RiskWarningHandleDTO;
import com.itheima.qukuailian.dto.YieldBalanceCheckDTO;
import com.itheima.qukuailian.entity.*;
import com.itheima.qukuailian.mapper.*;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceRegulationService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import com.itheima.qukuailian.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceRegulationServiceImpl implements RiceRegulationService {

    /** 亩产合理区间（kg/亩） */
    private static final BigDecimal YIELD_PER_MU_MIN = new BigDecimal("300");
    private static final BigDecimal YIELD_PER_MU_MAX = new BigDecimal("700");
    /** 精米产出率合理区间（%） */
    private static final BigDecimal MILLING_RATE_MIN = new BigDecimal("55");
    private static final BigDecimal MILLING_RATE_MAX = new BigDecimal("75");

    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceYieldBalanceMapper yieldBalanceMapper;
    private final RiceRiskWarningMapper riskWarningMapper;
    private final RiceFieldMapper fieldMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceTraceCodeMapper traceCodeMapper;
    private final RiceScanLogMapper scanLogMapper;
    private final RiceChainProofMapper chainProofMapper;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceYieldBalance checkYieldBalance(YieldBalanceCheckDTO dto, String ip) {
        String scope = dto.getCheckScope();
        List<Map<String, Object>> items = new ArrayList<>();
        boolean allPass = true;

        // 1. 种植 -> 入库：亩产校验（收割面积 vs 湿谷重量）
        if ("PLANTING_TO_STORAGE".equals(scope) || "FULL_CHAIN".equals(scope)) {
            RiceStorageReceipt receipt = findStorageReceipt(dto);
            if (receipt == null) {
                throw new BizException(ResultCode.VALIDATION_FAILED.getCode(), "未找到入库数据，无法校验");
            }
            BigDecimal yieldPerMu = receipt.getWetGrainWeightKg()
                    .divide(receipt.getHarvestAreaMu(), 2, RoundingMode.HALF_UP);
            boolean pass = yieldPerMu.compareTo(YIELD_PER_MU_MIN) >= 0
                    && yieldPerMu.compareTo(YIELD_PER_MU_MAX) <= 0;
            items.add(item("亩产校验", receipt.getHarvestAreaMu(), receipt.getWetGrainWeightKg(),
                    yieldPerMu, "kg/亩", "300-700kg/亩", pass));
            allPass = allPass && pass;
        }

        // 2. 入库 -> 加工：精米产出率校验
        if ("STORAGE_TO_MILLING".equals(scope) || "FULL_CHAIN".equals(scope)) {
            RiceMillingBatch milling = findMillingBatch(dto);
            if (milling == null) {
                throw new BizException(ResultCode.VALIDATION_FAILED.getCode(), "未找到加工批次数据，无法校验");
            }
            if (milling.getRiceOutputWeightKg() == null) {
                throw new BizException(ResultCode.VALIDATION_FAILED.getCode(),
                        "加工批次 " + milling.getMillingBatchId() + " 未完成，无法校验产出率");
            }
            BigDecimal yieldRate = milling.getRiceOutputWeightKg()
                    .divide(milling.getGrainOutWeightKg(), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            boolean pass = yieldRate.compareTo(MILLING_RATE_MIN) >= 0
                    && yieldRate.compareTo(MILLING_RATE_MAX) <= 0;
            items.add(item("精米产出率", milling.getGrainOutWeightKg(), milling.getRiceOutputWeightKg(),
                    yieldRate, "%", "55%-75%", pass));
            allPass = allPass && pass;
        }

        // 3. 保存校验结果 + 上链
        RiceYieldBalance check = new RiceYieldBalance();
        check.setCheckId(IdGen.generate("YBC"));
        check.setCheckScope(scope);
        check.setPlantingBatchId(dto.getPlantingBatchId());
        check.setGrainBatchId(dto.getGrainBatchId());
        check.setProductBatchId(dto.getProductBatchId());
        check.setResult(allPass ? "PASS" : "FAIL");
        check.setItems(items);
        check.setChainStatus("PENDING");
        yieldBalanceMapper.insert(check);
        chainProofService.submit("YIELD_BALANCE", check.getCheckId(), digest(check), null);

        // 4. 校验异常 -> 风险预警 + 审计日志（接口文档 §7.1）
        if (!allPass) {
            createYieldCheckWarning(check, items);
            auditLogService.record("YIELD_BALANCE", check.getCheckId(), "VERIFY", null,
                    digest(check), "产量平衡校验未通过", ip);
        }
        log.info("产量平衡校验: checkId={}, scope={}, result={}", check.getCheckId(), scope, check.getResult());
        return check;
    }

    @Override
    public IPage<RiceYieldBalance> pageYieldBalanceResults(long pageNo, long pageSize, String checkId,
                                                           String plantingBatchId, String grainBatchId,
                                                           String productBatchId, String result,
                                                           String startTime, String endTime) {
        LambdaQueryWrapper<RiceYieldBalance> wrapper = new LambdaQueryWrapper<>();
        LocalDateTime start = StringUtils.hasText(startTime) ? LocalDateTime.parse(startTime) : null;
        LocalDateTime end = StringUtils.hasText(endTime) ? LocalDateTime.parse(endTime) : null;
        wrapper.eq(StringUtils.hasText(checkId), RiceYieldBalance::getCheckId, checkId)
                .eq(StringUtils.hasText(plantingBatchId), RiceYieldBalance::getPlantingBatchId, plantingBatchId)
                .eq(StringUtils.hasText(grainBatchId), RiceYieldBalance::getGrainBatchId, grainBatchId)
                .eq(StringUtils.hasText(productBatchId), RiceYieldBalance::getProductBatchId, productBatchId)
                .eq(StringUtils.hasText(result), RiceYieldBalance::getResult, result)
                .ge(start != null, RiceYieldBalance::getCreateTime, start)
                .le(end != null, RiceYieldBalance::getCreateTime, end)
                .orderByDesc(RiceYieldBalance::getCreateTime);
        return yieldBalanceMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public IPage<RiceRiskWarning> pageRiskWarnings(long pageNo, long pageSize, String warningType,
                                                   String riskLevel, String businessId, Boolean handled) {
        LambdaQueryWrapper<RiceRiskWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(warningType), RiceRiskWarning::getWarningType, warningType)
                .eq(StringUtils.hasText(riskLevel), RiceRiskWarning::getRiskLevel, riskLevel)
                .eq(StringUtils.hasText(businessId), RiceRiskWarning::getBusinessId, businessId)
                .eq(handled != null, RiceRiskWarning::getHandled, handled)
                .orderByDesc(RiceRiskWarning::getCreateTime);
        return riskWarningMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRiskWarning handleWarning(String warningId, RiskWarningHandleDTO dto, String ip) {
        RiceRiskWarning warning = riskWarningMapper.selectById(warningId);
        if (warning == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "风险预警不存在: " + warningId);
        }
        if (Boolean.TRUE.equals(warning.getHandled())) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "预警已处理，不允许重复处理");
        }
        // 1. 处理结果 + 审计日志
        warning.setHandled(true);
        warning.setHandledBy(UserContext.getUsername());
        warning.setHandledAt(LocalDateTime.now());
        warning.setHandleResult(dto.getHandleResult());
        warning.setChainStatus("PENDING");
        riskWarningMapper.updateById(warning);
        auditLogService.record("RISK_WARNING", warningId, "HANDLE", null,
                digest(warning), dto.getReason(), ip);

        // 2. 处理摘要异步上链（失败不阻断主流程，接口文档 §7.4）
        chainProofService.submit("RISK_WARNING", warningId, digest(warning), null);
        log.info("风险预警处理完成: warningId={}, result={}", warningId, dto.getHandleResult());
        return warning;
    }

    @Override
    public Map<String, Object> dashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("fieldCount", fieldMapper.selectCount(null));
        summary.put("plantingBatchCount", plantingBatchMapper.selectCount(null));
        summary.put("storedGrainWeightKg", sumWetGrainWeight());
        summary.put("productBatchCount", productBatchMapper.selectCount(null));
        summary.put("traceCodeCount", traceCodeMapper.selectCount(null));
        summary.put("scanCount", scanLogMapper.selectCount(null));
        summary.put("riskWarningCount", riskWarningMapper.selectCount(null));
        summary.put("chainSuccessRate", chainSuccessRate());
        return summary;
    }

    private BigDecimal sumWetGrainWeight() {
        List<Map<String, Object>> rows = storageReceiptMapper.selectMaps(
                new QueryWrapper<RiceStorageReceipt>().select("IFNULL(SUM(wet_grain_weight_kg),0) AS total"));
        if (rows == null || rows.isEmpty() || rows.get(0).get("total") == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(rows.get(0).get("total").toString());
    }

    private BigDecimal chainSuccessRate() {
        Long total = chainProofMapper.selectCount(null);
        if (total == null || total == 0) {
            return new BigDecimal("100.00");
        }
        Long success = chainProofMapper.selectCount(
                new LambdaQueryWrapper<RiceChainProof>().eq(RiceChainProof::getChainStatus, "SUCCESS"));
        return BigDecimal.valueOf(success)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    // ---------- 私有辅助 ----------

    private RiceStorageReceipt findStorageReceipt(YieldBalanceCheckDTO dto) {
        if (StringUtils.hasText(dto.getGrainBatchId())) {
            return storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                    .eq(RiceStorageReceipt::getGrainBatchId, dto.getGrainBatchId()));
        }
        if (StringUtils.hasText(dto.getPlantingBatchId())) {
            return storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                    .eq(RiceStorageReceipt::getPlantingBatchId, dto.getPlantingBatchId())
                    .orderByDesc(RiceStorageReceipt::getStorageTime)
                    .last("LIMIT 1"));
        }
        return null;
    }

    private RiceMillingBatch findMillingBatch(YieldBalanceCheckDTO dto) {
        if (StringUtils.hasText(dto.getGrainBatchId())) {
            return millingBatchMapper.selectOne(new LambdaQueryWrapper<RiceMillingBatch>()
                    .eq(RiceMillingBatch::getGrainBatchId, dto.getGrainBatchId()));
        }
        if (StringUtils.hasText(dto.getProductBatchId())) {
            return millingBatchMapper.selectOne(new LambdaQueryWrapper<RiceMillingBatch>()
                    .eq(RiceMillingBatch::getProductBatchId, dto.getProductBatchId()));
        }
        return null;
    }

    private Map<String, Object> item(String name, BigDecimal input, BigDecimal output,
                                     BigDecimal computed, String unit, String threshold, boolean pass) {
        Map<String, Object> item = new HashMap<>();
        item.put("itemName", name);
        item.put("inputValue", input);
        item.put("outputValue", output);
        item.put("computedValue", computed);
        item.put("unit", unit);
        item.put("threshold", threshold);
        item.put("result", pass ? "PASS" : "FAIL");
        return item;
    }

    private void createYieldCheckWarning(RiceYieldBalance check, List<Map<String, Object>> items) {
        long failedCount = items.stream().filter(i -> "FAIL".equals(i.get("result"))).count();
        RiceRiskWarning warning = new RiceRiskWarning();
        warning.setWarningId(IdGen.generate("WARN"));
        warning.setWarningType("YIELD_BALANCE");
        warning.setRiskLevel(failedCount >= 2 ? "HIGH" : "MEDIUM");
        warning.setBusinessType("YIELD_BALANCE");
        warning.setBusinessId(check.getCheckId());
        warning.setWarningContent("产量平衡校验未通过(" + check.getCheckScope() + ")：" + items);
        warning.setHandled(false);
        warning.setChainStatus("PENDING");
        riskWarningMapper.insert(warning);
        log.warn("产量校验异常预警生成: warningId={}", warning.getWarningId());
    }

    private String digest(Object obj) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(obj));
        }
    }
}
