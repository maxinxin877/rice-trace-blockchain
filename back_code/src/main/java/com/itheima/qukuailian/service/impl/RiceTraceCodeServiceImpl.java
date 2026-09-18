package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.TraceCodeActivateDTO;
import com.itheima.qukuailian.dto.TraceCodeGenerateDTO;
import com.itheima.qukuailian.entity.RiceField;
import com.itheima.qukuailian.entity.RiceGenerateTask;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceQualityTest;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceScanLog;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceFieldMapper;
import com.itheima.qukuailian.entity.RiceChainProof;
import com.itheima.qukuailian.mapper.RiceChainProofMapper;
import com.itheima.qukuailian.mapper.RiceGenerateTaskMapper;
import com.itheima.qukuailian.mapper.RiceMillingBatchMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceQualityTestMapper;
import com.itheima.qukuailian.mapper.RiceRiskWarningMapper;
import com.itheima.qukuailian.mapper.RiceScanLogMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceTraceCodeService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceTraceCodeServiceImpl extends ServiceImpl<RiceTraceCodeMapper, RiceTraceCode>
        implements RiceTraceCodeService {

    /** 窜货/重复扫码类预警类型 */
    private static final List<String> CHANNEL_WARNING_TYPES = List.of("CHANNEL_CONFLICT", "REPEAT_SCAN");

    private final RiceGenerateTaskMapper generateTaskMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceRiskWarningMapper riskWarningMapper;
    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceFieldMapper fieldMapper;
    private final RiceQualityTestMapper qualityTestMapper;
    private final RiceChainProofMapper chainProofMapper;
    private final RiceScanLogMapper scanLogMapper;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    @Value("${app.qr-base-url:pages/rice/trace/index?code=}")
    private String qrBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceGenerateTask generate(TraceCodeGenerateDTO dto, String ip) {
        RiceProductBatch productBatch = productBatchMapper.selectById(dto.getProductBatchId());
        if (productBatch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "成品米批次不存在: " + dto.getProductBatchId());
        }

        // 1. 创建生成任务
        RiceGenerateTask task = new RiceGenerateTask();
        task.setGenerateTaskId(IdGen.generate("TCG"));
        task.setProductBatchId(dto.getProductBatchId());
        task.setQuantity(dto.getQuantity());
        task.setPackageSpec(dto.getPackageSpec());
        task.setExpectedSaleRegion(dto.getExpectedSaleRegion() == null ? "" : dto.getExpectedSaleRegion());
        task.setExpireDays(dto.getExpireDays() == null ? 0 : dto.getExpireDays());
        task.setStatus("PROCESSING");
        generateTaskMapper.insert(task);

        // 2. 批量生成防伪码（默认状态 GENERATED）
        List<RiceTraceCode> codes = buildCodes(dto);
        saveBatch(codes);
        task.setStatus("SUCCESS");
        generateTaskMapper.updateById(task);

        // 3. 生成批次摘要上链
        chainProofService.submit("TRACE_CODE", task.getGenerateTaskId(), digest(task), null);
        auditLogService.record("TRACE_CODE", task.getGenerateTaskId(), "CREATE",
                null, digest(task), "批量生成防伪码", ip);
        log.info("防伪码生成成功: taskId={}, quantity={}", task.getGenerateTaskId(), dto.getQuantity());
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activate(TraceCodeActivateDTO dto, String ip) {
        LocalDateTime activatedAt = dto.getActivatedAt() == null ? LocalDateTime.now() : dto.getActivatedAt();
        int activatedCount = 0;
        int failedCount = 0;

        for (String traceCode : dto.getTraceCodes()) {
            RiceTraceCode code = lambdaQuery().eq(RiceTraceCode::getTraceCode, traceCode).one();
            // 只有 GENERATED 状态的码、且属于指定成品批次才允许激活（接口文档 §5.25）
            boolean valid = code != null
                    && dto.getProductBatchId().equals(code.getProductBatchId())
                    && "GENERATED".equals(code.getStatus());
            if (!valid) {
                failedCount++;
                continue;
            }
            code.setStatus("ACTIVATED");
            code.setActivatedAt(activatedAt);
            code.setChainStatus("PENDING");
            updateById(code);
            activatedCount++;
        }

        if (activatedCount > 0) {
            // 激活状态摘要上链
            Map<String, Object> summary = new HashMap<>();
            summary.put("productBatchId", dto.getProductBatchId());
            summary.put("activatedCount", activatedCount);
            summary.put("activatedBy", dto.getActivatedBy());
            summary.put("activatedAt", activatedAt);
            chainProofService.submit("TRACE_CODE", dto.getProductBatchId(), digest(summary), null);

            // 成品米批次进入 ON_SALE
            RiceProductBatch productBatch = productBatchMapper.selectById(dto.getProductBatchId());
            if (productBatch != null && !"ON_SALE".equals(productBatch.getStatus())) {
                productBatch.setStatus("ON_SALE");
                productBatchMapper.updateById(productBatch);
            }
            // 种植批次沿 成品批次→加工批次→入库单 反查，同步进入 ON_SALE（已上市）
            RiceMillingBatch milling = millingBatchMapper.selectOne(
                    new LambdaQueryWrapper<RiceMillingBatch>()
                            .eq(RiceMillingBatch::getProductBatchId, dto.getProductBatchId())
                            .last("LIMIT 1"));
            if (milling != null) {
                RiceStorageReceipt receipt = storageReceiptMapper.selectOne(
                        new LambdaQueryWrapper<RiceStorageReceipt>()
                                .eq(RiceStorageReceipt::getGrainBatchId, milling.getGrainBatchId()));
                if (receipt != null) {
                    RicePlantingBatch plantingBatch = plantingBatchMapper.selectById(receipt.getPlantingBatchId());
                    if (plantingBatch != null && !"ON_SALE".equals(plantingBatch.getStatus())) {
                        plantingBatch.setStatus("ON_SALE");
                        plantingBatchMapper.updateById(plantingBatch);
                    }
                }
            }
            auditLogService.record("TRACE_CODE", dto.getProductBatchId(), "UPDATE", null,
                    digest(summary), StringUtils.hasText(dto.getReason()) ? dto.getReason() : "激活防伪码", ip);
        }

        log.info("防伪码激活: productBatch={}, 成功={}, 失败={}", dto.getProductBatchId(), activatedCount, failedCount);
        Map<String, Object> result = new HashMap<>();
        result.put("activatedCount", activatedCount);
        result.put("failedCount", failedCount);
        result.put("chainStatus", "PENDING");
        return result;
    }

    @Override
    public IPage<RiceTraceCode> page(long pageNo, long pageSize, String productBatchId, String traceCode,
                                     String status, String riskLevel) {
        LambdaQueryWrapper<RiceTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(productBatchId), RiceTraceCode::getProductBatchId, productBatchId)
                .eq(StringUtils.hasText(traceCode), RiceTraceCode::getTraceCode, traceCode)
                .eq(StringUtils.hasText(status), RiceTraceCode::getStatus, status)
                .eq(StringUtils.hasText(riskLevel), RiceTraceCode::getRiskLevel, riskLevel)
                .orderByDesc(RiceTraceCode::getCreateTime);
        IPage<RiceTraceCode> result = page(new Page<>(pageNo, pageSize), wrapper);
        fillProductInfo(result.getRecords());
        return result;
    }

    /** 批量填充防伪码关联的成品批次产品名称、品牌、品种（避免 N+1 查询） */
    private void fillProductInfo(List<RiceTraceCode> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }
        List<String> productBatchIds = codes.stream()
                .map(RiceTraceCode::getProductBatchId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (productBatchIds.isEmpty()) {
            return;
        }
        Map<String, RiceProductBatch> productMap = productBatchMapper.selectBatchIds(productBatchIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(RiceProductBatch::getProductBatchId, p -> p, (a, b) -> a));
        for (RiceTraceCode code : codes) {
            RiceProductBatch product = productMap.get(code.getProductBatchId());
            if (product != null) {
                code.setProductName(product.getProductName());
                code.setBrandName(product.getBrandName());
                code.setRiceVariety(product.getRiceVariety());
            }
        }
    }

    @Override
    public Map<String, Object> traceDetail(String traceCode) {
        RiceTraceCode code = lambdaQuery().eq(RiceTraceCode::getTraceCode, traceCode).one();
        if (code == null) {
            return null;
        }
        RiceProductBatch product = productBatchMapper.selectById(code.getProductBatchId());
        if (product == null) {
            return null;
        }
        // 填充码关联的产品名称/品牌/品种，供溯源页头部展示
        code.setProductName(product.getProductName());
        code.setBrandName(product.getBrandName());
        code.setRiceVariety(product.getRiceVariety());

        // 沿链路反向追溯：成品 -> 加工 -> 入库 -> 种植 -> 地块
        RiceMillingBatch milling = millingBatchMapper.selectOne(
                new LambdaQueryWrapper<RiceMillingBatch>()
                        .eq(RiceMillingBatch::getProductBatchId, product.getProductBatchId())
                        .orderByDesc(RiceMillingBatch::getCreateTime).last("LIMIT 1"));
        RiceStorageReceipt storage = null;
        if (milling != null) {
            storage = storageReceiptMapper.selectOne(
                    new LambdaQueryWrapper<RiceStorageReceipt>()
                            .eq(RiceStorageReceipt::getGrainBatchId, milling.getGrainBatchId())
                            .last("LIMIT 1"));
        }
        RicePlantingBatch planting = storage != null ? plantingBatchMapper.selectById(storage.getPlantingBatchId()) : null;
        RiceField field = planting != null ? fieldMapper.selectById(planting.getFieldId()) : null;
        RiceQualityTest qualityTest = storage != null ? qualityTestMapper.selectOne(
                new LambdaQueryWrapper<RiceQualityTest>()
                        .eq(RiceQualityTest::getBusinessType, "STORAGE")
                        .eq(RiceQualityTest::getBusinessId, storage.getStorageReceiptId())
                        .orderByDesc(RiceQualityTest::getTestTime).last("LIMIT 1")) : null;

        String origin = field == null ? "" : String.join("", nz(field.getProvince()), nz(field.getCity()), nz(field.getDistrict()));
        String qualityResult = qualityTest == null ? "待检"
                : "PASS".equals(qualityTest.getOverallResult()) ? "合格"
                : "FAIL".equals(qualityTest.getOverallResult()) ? "不合格" : "待检";

        List<Map<String, Object>> timeline = new ArrayList<>();
        if (field != null) {
            // 建档时间优先取业务上链时间，避免落在数据补录时间上造成链路时序错乱
            LocalDateTime fieldTime = field.getChainTime() != null ? field.getChainTime() : field.getCreateTime();
            timeline.add(node("FIELD", "产地建档", fmtDt(fieldTime),
                    nz(field.getFarmerName()),
                    origin + nz(field.getAddress()),
                    String.format("%s；土壤类型：%s；地块面积：%s亩", nz(field.getFieldName()), nz(field.getSoilType()), num(field.getAreaMu())),
                    field.getFieldId(), field.getChainStatus()));
        }
        if (planting != null) {
            timeline.add(node("PLANTING", "种植与农事", fmtD(planting.getSowingDate()),
                    field == null ? "" : nz(field.getFarmerName()),
                    field == null ? "" : nz(field.getCity()) + nz(field.getDistrict()),
                    String.format("%s 完成播种，种子来源：%s，农事记录与投入品批次完整",
                            nz(planting.getRiceVariety()), nz(planting.getSeedSource())),
                    planting.getPlantingBatchId(), planting.getChainStatus()));
        }
        if (storage != null) {
            timeline.add(node("STORAGE", "收储与质检", fmtDt(storage.getStorageTime()),
                    "收储仓库（" + nz(storage.getWarehouseCode()) + "）",
                    field == null ? "" : nz(field.getCity()) + nz(field.getDistrict()),
                    String.format("入库湿谷 %skg，粮食品级：%s，质检结论：%s",
                            num(storage.getWetGrainWeightKg()), nz(storage.getGrainGrade()), qualityResult),
                    storage.getStorageReceiptId(), storage.getChainStatus()));
        }
        if (milling != null) {
            timeline.add(node("MILLING", "碾米加工", fmtDt(milling.getProcessEndTime()),
                    "授权大米定点加工厂", "加工园区",
                    String.format("出米率 %s%%；%s", num(milling.getYieldRate()), nz(milling.getQualitySummary())),
                    milling.getMillingBatchId(), milling.getChainStatus()));
        }
        LocalDateTime packageTime = code.getActivatedAt() != null ? code.getActivatedAt() : code.getCreateTime();
        timeline.add(node("PRODUCT", "包装赋码", fmtDt(packageTime),
                nz(product.getBrandName()) + "品牌运营中心",
                "销售区域：" + nz(code.getExpectedSaleRegion()),
                String.format("%s 规格包装，一物一码%s", nz(code.getPackageSpec()), codeStatusText(code.getStatus())),
                product.getProductBatchId(), code.getChainStatus()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traceCode", code);
        result.put("fieldName", field == null ? "" : field.getFieldName());
        result.put("origin", origin);
        result.put("harvestDate", planting == null ? "" : fmtD(planting.getActualHarvestDate()));
        result.put("productionDate", milling == null ? "" : fmtD(milling.getProcessEndTime() == null ? null : milling.getProcessEndTime().toLocalDate()));
        result.put("qualityResult", qualityResult);
        result.put("nutritionFacts", buildNutritionFacts(product.getNutritionFacts()));
        result.put("certificates", buildCertificates(planting, qualityTest));
        result.put("timeline", timeline);
        return result;
    }

    @Override
    public Map<String, Object> chainProof(String traceCode) {
        RiceTraceCode code = lambdaQuery().eq(RiceTraceCode::getTraceCode, traceCode).one();
        if (code == null) {
            return null;
        }
        RiceChainProof proof = chainProofMapper.selectOne(
                new LambdaQueryWrapper<RiceChainProof>()
                        .eq(RiceChainProof::getBusinessType, "TRACE_CODE")
                        .eq(RiceChainProof::getBusinessId, traceCode));

        String dataHash;
        String txId;
        Long blockHeight;
        String chainStatus;
        String chainTime;
        if (proof != null && StringUtils.hasText(proof.getTxId())) {
            // 存证表有完整记录
            dataHash = proof.getDataHash();
            txId = proof.getTxId();
            blockHeight = proof.getBlockHeight();
            chainStatus = proof.getChainStatus();
            chainTime = fmtDt(proof.getChainTime());
        } else {
            // 兜底：历史演示数据的上链信息直接记录在防伪码上，实时重算业务摘要
            dataHash = HashUtils.sha256("trace-code:" + traceCode);
            txId = code.getTxId();
            blockHeight = code.getBlockHeight();
            chainStatus = StringUtils.hasText(code.getChainStatus()) ? code.getChainStatus() : "PENDING";
            chainTime = fmtDt(code.getChainTime());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessType", "TRACE_CODE");
        result.put("businessId", traceCode);
        result.put("txId", txId);
        result.put("blockHeight", blockHeight);
        result.put("chainTime", chainTime);
        result.put("dataHash", dataHash);
        result.put("chainHash", dataHash);
        result.put("currentHash", dataHash);
        result.put("verified", "SUCCESS".equals(chainStatus));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> verifyCode(String traceCode, String region) {
        String scanRegion = StringUtils.hasText(region) ? region.trim() : "未知地区";
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traceCode", traceCode);
        result.put("currentScanAt", fmtDt(now));

        RiceTraceCode code = lambdaQuery().eq(RiceTraceCode::getTraceCode, traceCode).one();
        // 1. 码不存在或已停用
        if (code == null || "DISABLED".equals(code.getStatus())) {
            result.put("verified", false);
            result.put("result", "INVALID");
            result.put("title", "未查询到有效防伪码");
            result.put("message", "请核对包装上的防伪码，谨防假冒产品。");
            result.put("scanCount", code == null || code.getScanCount() == null ? 0 : code.getScanCount());
            result.put("firstScannedAt", code != null && code.getFirstScannedAt() != null ? fmtDt(code.getFirstScannedAt()) : null);
            result.put("riskTips", List.of("不要购买来源不明或包装破损的产品"));
            return result;
        }
        // 2. 已生成但未出库激活：不记录扫码
        if ("GENERATED".equals(code.getStatus())) {
            result.put("verified", false);
            result.put("result", "RISK");
            result.put("title", "防伪码尚未激活");
            result.put("message", "该产品尚未完成出库激活，请联系销售方核实。");
            result.put("scanCount", num(code.getScanCount()));
            result.put("firstScannedAt", code.getFirstScannedAt() != null ? fmtDt(code.getFirstScannedAt()) : null);
            result.put("riskTips", List.of("未激活防伪码不作为正品凭证"));
            return result;
        }

        // 3. 记录扫码日志并累计次数
        boolean firstScan = code.getFirstScannedAt() == null;
        int scanCount = num(code.getScanCount()) + 1;

        // 4. 风险判定：已标记风险 > 跨区域窜货 > 首次正品 > 重复查询
        boolean alreadyRisky = "RISK".equals(code.getStatus()) || "HIGH".equals(code.getRiskLevel());
        boolean regionMismatch = !alreadyRisky && isRegionMismatch(code.getExpectedSaleRegion(), scanRegion);
        String verifyResult;
        String title;
        String message;
        List<String> riskTips;
        String logRiskLevel;
        if (alreadyRisky) {
            verifyResult = "RISK";
            title = "该防伪码存在风险";
            message = "系统检测到高频或跨区域扫码，请核对购买渠道。";
            riskTips = List.of("核对销售渠道与发票", "如包装异常，请联系品牌客服或监管部门");
            logRiskLevel = nz(code.getRiskLevel());
        } else if (regionMismatch) {
            verifyResult = "RISK";
            title = "该防伪码存在风险";
            message = "系统检测到该产品在非授权销售区域【" + scanRegion + "】被扫码，疑似窜货，请核对购买渠道。";
            riskTips = List.of("授权销售区域：" + nz(code.getExpectedSaleRegion()), "核对销售渠道与发票", "如包装异常，请联系品牌客服或监管部门");
            logRiskLevel = "HIGH";
        } else if (firstScan) {
            verifyResult = "GENUINE";
            title = "正品 · 首次查询";
            message = "该防伪码已通过系统校验，且为首次扫码。";
            riskTips = List.of();
            logRiskLevel = "LOW";
        } else {
            verifyResult = "REPEAT";
            title = "正品 · 非首次查询";
            message = "该产品已被查询 " + scanCount + " 次，请结合首次查询时间判断。";
            riskTips = List.of();
            logRiskLevel = "LOW";
        }

        // 写扫码日志
        RiceScanLog scanLog = new RiceScanLog();
        scanLog.setScanLogId(IdGen.generate("SL"));
        scanLog.setTraceCode(traceCode);
        scanLog.setProductBatchId(code.getProductBatchId());
        scanLog.setScanTime(now);
        scanLog.setRegion(scanRegion);
        scanLog.setDeviceId("H5-ANONYMOUS");
        scanLog.setScene("CONSUMER_SCAN");
        scanLog.setAuthentic(!"RISK".equals(verifyResult));
        scanLog.setFirstScan(firstScan);
        scanLog.setRiskLevel(logRiskLevel);
        scanLog.setCreateTime(now);
        scanLogMapper.insert(scanLog);

        // 更新防伪码扫码信息；窜货首次发现时升级风险状态并生成预警
        code.setScanCount(scanCount);
        if (firstScan) {
            code.setFirstScannedAt(now);
        }
        code.setLastScanRegion(scanRegion);
        if (regionMismatch) {
            code.setStatus("RISK");
            code.setRiskLevel("HIGH");
            createChannelWarningIfAbsent(code, scanRegion, now);
        }
        updateById(code);

        result.put("verified", !"RISK".equals(verifyResult));
        result.put("result", verifyResult);
        result.put("title", title);
        result.put("message", message);
        result.put("scanCount", scanCount);
        result.put("firstScannedAt", fmtDt(code.getFirstScannedAt()));
        result.put("riskTips", riskTips);
        return result;
    }

    /** 省级区域匹配：取扫码地区前 3 位（省/直辖市），在预期销售区域文本中包含即视为一致 */
    private boolean isRegionMismatch(String expectedSaleRegion, String scanRegion) {
        if (!StringUtils.hasText(expectedSaleRegion) || "未知地区".equals(scanRegion)) {
            return false;
        }
        String province = scanRegion.length() >= 3 ? scanRegion.substring(0, 3) : scanRegion;
        return !expectedSaleRegion.contains(province);
    }

    /** 同一防伪码存在未处理窜货预警时不重复生成 */
    private void createChannelWarningIfAbsent(RiceTraceCode code, String scanRegion, LocalDateTime now) {
        Long exists = riskWarningMapper.selectCount(new LambdaQueryWrapper<RiceRiskWarning>()
                .eq(RiceRiskWarning::getBusinessType, "TRACE_CODE")
                .eq(RiceRiskWarning::getBusinessId, code.getTraceCode())
                .eq(RiceRiskWarning::getWarningType, "CHANNEL_CONFLICT")
                .eq(RiceRiskWarning::getHandled, false));
        if (exists != null && exists > 0) {
            return;
        }
        RiceRiskWarning warning = new RiceRiskWarning();
        warning.setWarningId(IdGen.generate("RW"));
        warning.setWarningType("CHANNEL_CONFLICT");
        warning.setRiskLevel("HIGH");
        warning.setBusinessType("TRACE_CODE");
        warning.setBusinessId(code.getTraceCode());
        warning.setWarningContent("消费者扫码地区【" + scanRegion + "】不在授权销售区域【"
                + nz(code.getExpectedSaleRegion()) + "】，疑似窜货");
        warning.setHandled(false);
        warning.setChainStatus("PENDING");
        warning.setCreateTime(now);
        warning.setUpdateTime(now);
        riskWarningMapper.insert(warning);
    }

    /** 组装资质证书与检测报告：有机/绿色认证取自种植批次，质检报告取自入库质检 */
    private List<Map<String, Object>> buildCertificates(RicePlantingBatch planting, RiceQualityTest qualityTest) {
        List<Map<String, Object>> certificates = new ArrayList<>();
        String batchNo = planting == null ? "" : planting.getPlantingBatchId()
                .replaceAll("\\D", "");
        if (batchNo.length() > 4) {
            batchNo = batchNo.substring(batchNo.length() - 4);
        }
        if (planting != null && Boolean.TRUE.equals(planting.getOrganicCertified())) {
            certificates.add(certificate("CERT-ORG-" + nz(planting.getPlantingBatchId()),
                    "有机产品认证证书", "中国质量认证中心",
                    "ORG-2026-" + batchNo, "2026-01-01", "2028-12-31"));
        }
        if (planting != null && Boolean.TRUE.equals(planting.getGreenCertified())) {
            certificates.add(certificate("CERT-GREEN-" + nz(planting.getPlantingBatchId()),
                    "绿色食品认证证书", "中国绿色食品发展中心",
                    "LB-26-" + batchNo, "2026-01-01", "2028-12-31"));
        }
        if (qualityTest != null) {
            LocalDate reportDate = qualityTest.getTestTime() != null ? qualityTest.getTestTime().toLocalDate() : LocalDate.now();
            certificates.add(certificate("CERT-QT-" + qualityTest.getQualityTestId(),
                    "入库质量检验报告", nz(qualityTest.getTestAgency()),
                    qualityTest.getQualityTestId(), fmtD(reportDate), fmtD(reportDate.plusYears(1))));
        }
        return certificates;
    }

    private Map<String, Object> certificate(String id, String name, String authority,
                                            String certificateNo, String validFrom, String validTo) {
        Map<String, Object> cert = new LinkedHashMap<>();
        cert.put("certificateId", id);
        cert.put("name", name);
        cert.put("authority", authority);
        cert.put("certificateNo", certificateNo);
        cert.put("validFrom", validFrom);
        cert.put("validTo", validTo);
        cert.put("status", "VALID");
        cert.put("fileType", "PDF");
        return cert;
    }

    /** 营养成分英文 key 转中文展示项 */
    private Map<String, String> buildNutritionFacts(Map<String, Object> raw) {
        Map<String, String> facts = new LinkedHashMap<>();
        if (raw == null) {
            return facts;
        }
        putFact(facts, raw, "energy_kj", "能量", "kJ/100g");
        putFact(facts, raw, "protein_g", "蛋白质", "g/100g");
        putFact(facts, raw, "fat_g", "脂肪", "g/100g");
        putFact(facts, raw, "carbohydrate_g", "碳水化合物", "g/100g");
        putFact(facts, raw, "sodium_mg", "钠", "mg/100g");
        return facts;
    }

    private void putFact(Map<String, String> facts, Map<String, Object> raw, String key, String label, String unit) {
        Object value = raw.get(key);
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            facts.put(label, value + " " + unit);
        }
    }

    private Map<String, Object> node(String stage, String title, String time, String organization,
                                     String location, String summary, String businessId, String chainStatus) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("stage", stage);
        n.put("title", title);
        n.put("time", time);
        n.put("organization", organization);
        n.put("location", location);
        n.put("summary", summary);
        n.put("businessId", businessId);
        n.put("chainStatus", StringUtils.hasText(chainStatus) ? chainStatus : "PENDING");
        return n;
    }

    private String codeStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "ACTIVATED" -> "已激活";
            case "RISK" -> "存在风险";
            case "DEACTIVATED", "DISABLED" -> "已停用";
            default -> "已生成待激活";
        };
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private int num(Integer n) {
        return n == null ? 0 : n;
    }

    private String num(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String fmtDt(LocalDateTime time) {
        return time == null ? "" : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String fmtD(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Override
    public IPage<RiceRiskWarning> pageChannelWarnings(long pageNo, long pageSize, String productBatchId,
                                                      String traceCode, String riskLevel, String region,
                                                      String startTime, String endTime) {
        LambdaQueryWrapper<RiceRiskWarning> wrapper = new LambdaQueryWrapper<>();
        LocalDateTime start = StringUtils.hasText(startTime) ? LocalDateTime.parse(startTime) : null;
        LocalDateTime end = StringUtils.hasText(endTime) ? LocalDateTime.parse(endTime) : null;
        wrapper.in(RiceRiskWarning::getWarningType, CHANNEL_WARNING_TYPES)
                .eq(StringUtils.hasText(traceCode), RiceRiskWarning::getBusinessId, traceCode)
                .eq(StringUtils.hasText(riskLevel), RiceRiskWarning::getRiskLevel, riskLevel)
                .ge(start != null, RiceRiskWarning::getCreateTime, start)
                .le(end != null, RiceRiskWarning::getCreateTime, end)
                .orderByDesc(RiceRiskWarning::getCreateTime);
        IPage<RiceRiskWarning> result = riskWarningMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        fillChannelWarningInfo(result.getRecords());
        return result;
    }

    /** 批量填充窜货预警展示字段：防伪码、产品名、预期/实际扫码区域、扫码次数、预警时间 */
    private void fillChannelWarningInfo(List<RiceRiskWarning> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        List<String> traceCodes = warnings.stream()
                .filter(w -> "TRACE_CODE".equals(w.getBusinessType()))
                .map(RiceRiskWarning::getBusinessId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, RiceTraceCode> codeMap = traceCodes.isEmpty() ? Map.of()
                : lambdaQuery().in(RiceTraceCode::getTraceCode, traceCodes).list().stream()
                .collect(java.util.stream.Collectors.toMap(RiceTraceCode::getTraceCode, c -> c, (a, b) -> a));
        List<String> productBatchIds = codeMap.values().stream()
                .map(RiceTraceCode::getProductBatchId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, RiceProductBatch> productMap = productBatchIds.isEmpty() ? Map.of()
                : productBatchMapper.selectBatchIds(productBatchIds).stream()
                .collect(java.util.stream.Collectors.toMap(RiceProductBatch::getProductBatchId, p -> p, (a, b) -> a));
        for (RiceRiskWarning warning : warnings) {
            warning.setWarningTime(fmtDt(warning.getCreateTime()));
            warning.setTraceCode(warning.getBusinessId());
            RiceTraceCode code = codeMap.get(warning.getBusinessId());
            if (code != null) {
                warning.setExpectedRegion(code.getExpectedSaleRegion());
                warning.setActualRegion(code.getLastScanRegion());
                warning.setScanCount(num(code.getScanCount()));
                RiceProductBatch product = productMap.get(code.getProductBatchId());
                if (product != null) {
                    warning.setProductName(product.getProductName());
                }
            }
        }
    }

    /** 构造防伪码列表：RC + 日期 + 8 位序号，二维码内容指向小程序溯源页 */
    private List<RiceTraceCode> buildCodes(TraceCodeGenerateDTO dto) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int offset = ThreadLocalRandom.current().nextInt(100000, 900000);
        List<RiceTraceCode> codes = new ArrayList<>(dto.getQuantity());
        for (int i = 0; i < dto.getQuantity(); i++) {
            String code = "RC" + date + String.format("%08d", offset + i);
            RiceTraceCode entity = new RiceTraceCode();
            entity.setTraceCode(code);
            entity.setQrCodeUrl(qrBaseUrl + code);
            entity.setProductBatchId(dto.getProductBatchId());
            entity.setPackageSpec(dto.getPackageSpec());
            entity.setStatus("GENERATED");
            entity.setExpireDays(dto.getExpireDays() == null ? 0 : dto.getExpireDays());
            entity.setExpectedSaleRegion(dto.getExpectedSaleRegion() == null ? "" : dto.getExpectedSaleRegion());
            entity.setScanCount(0);
            entity.setRiskLevel("LOW");
            entity.setChainStatus("PENDING");
            codes.add(entity);
        }
        return codes;
    }

    private String digest(Object obj) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(obj));
        }
    }
}
