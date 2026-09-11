package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.entity.*;
import com.itheima.qukuailian.mapper.*;
import com.itheima.qukuailian.service.RiceMiniTraceService;
import com.itheima.qukuailian.utils.IdGen;
import com.itheima.qukuailian.vo.MiniChainProofVO;
import com.itheima.qukuailian.vo.MiniTraceDetailVO;
import com.itheima.qukuailian.vo.MiniVerifyResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 小程序扫码溯源实现（接口文档 v1.1 §6.2-§6.4）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiceMiniTraceServiceImpl implements RiceMiniTraceService {

    /** 重复扫码预警阈值 */
    private static final int REPEAT_SCAN_THRESHOLD = 5;
    /** 高风险重复扫码阈值 */
    private static final int REPEAT_SCAN_HIGH_THRESHOLD = 20;

    private final RiceTraceCodeMapper traceCodeMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceFieldMapper fieldMapper;
    private final RiceFarmingLogMapper farmingLogMapper;
    private final RiceQualityTestMapper qualityTestMapper;
    private final RiceQualityTestItemMapper qualityTestItemMapper;
    private final RiceScanLogMapper scanLogMapper;
    private final RiceRiskWarningMapper riskWarningMapper;
    private final RiceChainProofMapper chainProofMapper;
    private final FileResourceMapper fileResourceMapper;

    @Override
    public MiniTraceDetailVO getTraceDetail(String traceCode) {
        RiceTraceCode code = findCodeOrNull(traceCode);
        if (code == null) {
            return null; // 防伪码不存在：由前端展示"未查询到"空态
        }
        RiceProductBatch productBatch = productBatchMapper.selectById(code.getProductBatchId());

        MiniTraceDetailVO vo = new MiniTraceDetailVO();
        vo.setTraceCode(traceCode);
        vo.setProduct(productOf(productBatch));

        // 真伪鉴别摘要（不增加扫码次数）
        Map<String, Object> authenticity = new LinkedHashMap<>();
        authenticity.put("status", code.getStatus());
        authenticity.put("scanCount", code.getScanCount() == null ? 0 : code.getScanCount());
        authenticity.put("displayText", switch (code.getStatus()) {
            case "ACTIVATED" -> code.getScanCount() != null && code.getScanCount() > 0 ? "已查询" : "待验真";
            case "GENERATED" -> "尚未激活";
            case "DISABLED" -> "已停用";
            default -> "存在风险";
        });
        authenticity.put("riskLevel", code.getRiskLevel());
        vo.setAuthenticity(authenticity);

        // 全链路：加工 -> 入库 -> 种植 -> 地块
        RiceMillingBatch milling = millingBatchMapper.selectOne(new LambdaQueryWrapper<RiceMillingBatch>()
                .eq(RiceMillingBatch::getProductBatchId, code.getProductBatchId())
                .orderByDesc(RiceMillingBatch::getCreateTime).last("LIMIT 1"));
        RiceStorageReceipt storage = null;
        RicePlantingBatch planting = null;
        RiceField field = null;
        if (milling != null) {
            storage = storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                    .eq(RiceStorageReceipt::getGrainBatchId, milling.getGrainBatchId()));
        }
        if (storage != null) {
            planting = plantingBatchMapper.selectById(storage.getPlantingBatchId());
        }
        if (planting != null) {
            field = fieldMapper.selectById(planting.getFieldId());
        }

        vo.setPlanting(plantingOf(planting, field));
        vo.setFarmingLogs(planting == null ? List.of()
                : farmingLogsOf(planting.getPlantingBatchId()));
        vo.setStorage(storageOf(storage));
        vo.setMilling(millingOf(milling, storage));
        vo.setCertifications(certificationsOf(productBatch));
        vo.setChainProof(chainProofMapOf(code, milling, storage));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniVerifyResultVO verify(String traceCode, BigDecimal lng, BigDecimal lat,
                                     String region, String deviceId, String scene) {
        RiceTraceCode code = findCodeOrNull(traceCode);
        MiniVerifyResultVO result = new MiniVerifyResultVO();
        result.setTraceCode(traceCode);
        if (code == null) {
            // 防伪码不存在：返回"未查询到有效防伪码"，不记录扫码日志
            result.setAuthentic(false);
            result.setFirstScan(false);
            result.setScanCount(0);
            result.setDisplayText("未查询到有效防伪码，请核对包装上的防伪码");
            result.setRiskLevel("MEDIUM");
            result.setChannelWarning(false);
            result.setChainVerified(false);
            result.setCodeStatus("INVALID");
            result.setCurrentScanAt(LocalDateTime.now());
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean activated = "ACTIVATED".equals(code.getStatus());
        boolean expired = isExpired(code, now);
        boolean firstScan = code.getFirstScannedAt() == null;
        int scanCount = (code.getScanCount() == null ? 0 : code.getScanCount()) + 1;
        boolean authentic = activated && !expired;
        result.setAuthentic(authentic);
        result.setFirstScan(firstScan);
        result.setScanCount(scanCount);

        // 风险判定：跨区扫码 / 重复扫码 / 未激活 / 停用
        boolean channelConflict = isChannelConflict(code, region);
        String riskLevel = "LOW";
        if ("RISK".equals(code.getStatus()) || scanCount > REPEAT_SCAN_HIGH_THRESHOLD) {
            riskLevel = "HIGH";
        } else if (channelConflict || scanCount > REPEAT_SCAN_THRESHOLD || "DISABLED".equals(code.getStatus())) {
            riskLevel = "MEDIUM";
        }
        result.setRiskLevel(riskLevel);
        result.setChannelWarning(channelConflict);
        result.setDisplayText(displayTextOf(activated, expired, firstScan, scanCount));
        result.setCodeStatus(code.getStatus());
        result.setFirstScannedAt(firstScan && authentic ? now : code.getFirstScannedAt());
        result.setCurrentScanAt(now);

        // 更新防伪码扫码信息
        code.setScanCount(scanCount);
        code.setLastScanRegion(region == null ? "" : region);
        if (firstScan && authentic) {
            code.setFirstScannedAt(now);
        }
        if (!"LOW".equals(riskLevel) && "LOW".equals(code.getRiskLevel())) {
            code.setRiskLevel(riskLevel);
        }
        traceCodeMapper.updateById(code);

        // 写扫码日志
        RiceScanLog scanLog = new RiceScanLog();
        scanLog.setScanLogId(IdGen.generate("SCAN"));
        scanLog.setTraceCode(traceCode);
        scanLog.setProductBatchId(code.getProductBatchId());
        scanLog.setScanTime(now);
        scanLog.setLng(lng);
        scanLog.setLat(lat);
        scanLog.setRegion(region == null ? "" : region);
        scanLog.setDeviceId(deviceId == null ? "" : deviceId);
        scanLog.setScene(scene == null ? "CONSUMER_SCAN" : scene);
        scanLog.setAuthentic(authentic);
        scanLog.setFirstScan(firstScan);
        scanLog.setRiskLevel(riskLevel);
        scanLogMapper.insert(scanLog);

        // 风险预警
        if (channelConflict) {
            createWarning("CHANNEL_CONFLICT", "MEDIUM", "TRACE_CODE", traceCode,
                    String.format("防伪码 %s 在 %s 扫码，超出预期销售区域 %s",
                            traceCode, region, code.getExpectedSaleRegion()));
        }
        if (scanCount > REPEAT_SCAN_THRESHOLD) {
            createWarning("REPEAT_SCAN", scanCount > REPEAT_SCAN_HIGH_THRESHOLD ? "HIGH" : "MEDIUM",
                    "TRACE_CODE", traceCode,
                    String.format("防伪码 %s 累计扫码 %d 次，超过阈值 %d", traceCode, scanCount, REPEAT_SCAN_THRESHOLD));
        }
        if (!activated) {
            createWarning("REPEAT_SCAN", "MEDIUM", "TRACE_CODE", traceCode,
                    String.format("未激活防伪码 %s 被扫码（状态 %s）", traceCode, code.getStatus()));
        }

        result.setChainVerified(isChainVerified(code.getProductBatchId()));
        return result;
    }

    @Override
    public MiniChainProofVO chainProof(String traceCode) {
        MiniChainProofVO vo = new MiniChainProofVO();
        vo.setTraceCode(traceCode);
        RiceTraceCode code = findCodeOrNull(traceCode);
        if (code == null) {
            vo.setVerified(false);
            return vo;
        }
        RiceMillingBatch milling = millingBatchMapper.selectOne(new LambdaQueryWrapper<RiceMillingBatch>()
                .eq(RiceMillingBatch::getProductBatchId, code.getProductBatchId())
                .orderByDesc(RiceMillingBatch::getCreateTime).last("LIMIT 1"));

        vo.setProductBatchId(code.getProductBatchId());

        RiceChainProof proof = latestProof(code, milling);
        if (proof == null) {
            vo.setVerified(false);
            return vo;
        }
        vo.setVerified("SUCCESS".equals(proof.getChainStatus()));
        vo.setDataHash(proof.getDataHash());
        vo.setChainHash(proof.getDataHash());
        vo.setTxId(proof.getTxId());
        vo.setBlockHeight(proof.getBlockHeight());
        vo.setChainTime(proof.getChainTime());
        return vo;
    }

    // ---------------- 私有辅助 ----------------

    private RiceTraceCode findCodeOrNull(String traceCode) {
        return traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                .eq(RiceTraceCode::getTraceCode, traceCode));
    }

    @SuppressWarnings("unused")
    private RiceTraceCode findCode(String traceCode) {
        RiceTraceCode code = findCodeOrNull(traceCode);
        if (code == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "防伪码不存在: " + traceCode);
        }
        return code;
    }

    private boolean isExpired(RiceTraceCode code, LocalDateTime now) {
        Integer expireDays = code.getExpireDays();
        if (expireDays == null || expireDays <= 0 || code.getActivatedAt() == null) {
            return false;
        }
        return code.getActivatedAt().plusDays(expireDays).isBefore(now);
    }

    private boolean isChannelConflict(RiceTraceCode code, String region) {
        String expected = code.getExpectedSaleRegion();
        if (!StringUtils.hasText(region) || !StringUtils.hasText(expected)) {
            return false;
        }
        for (String item : expected.split("[,，]")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty() && (region.contains(trimmed) || trimmed.contains(region))) {
                return false;
            }
        }
        return true;
    }

    private String displayTextOf(boolean activated, boolean expired, boolean firstScan, int scanCount) {
        if (expired) {
            return "该防伪码已过期，请谨慎购买";
        }
        if (!activated) {
            return "该防伪码尚未激活，请核对销售渠道";
        }
        return firstScan ? "正品" : "该产品已被查询" + scanCount + "次";
    }

    private Map<String, Object> productOf(RiceProductBatch productBatch) {
        if (productBatch == null) {
            return null;
        }
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("productBatchId", productBatch.getProductBatchId());
        product.put("productName", productBatch.getProductName());
        product.put("brandName", productBatch.getBrandName());
        product.put("packageSpec", productBatch.getPackageSpec());
        product.put("standardNo", productBatch.getStandardNo());
        product.put("riceVariety", productBatch.getRiceVariety());
        product.put("nutritionFacts", productBatch.getNutritionFacts());
        product.put("expectedSaleRegion", productBatch.getExpectedSaleRegion());
        product.put("status", productBatch.getStatus());
        return product;
    }

    private Map<String, Object> plantingOf(RicePlantingBatch planting, RiceField field) {
        if (planting == null && field == null) {
            return null;
        }
        Map<String, Object> plantingMap = new LinkedHashMap<>();
        if (field != null) {
            plantingMap.put("fieldName", field.getFieldName());
            plantingMap.put("address", field.getProvince() + field.getCity() + field.getDistrict() + field.getAddress());
            plantingMap.put("areaMu", field.getAreaMu());
            plantingMap.put("basePhotos", fileUrlsOf(field.getBasePhotoFileIds()));
        }
        if (planting != null) {
            plantingMap.put("plantingBatchId", planting.getPlantingBatchId());
            plantingMap.put("riceVariety", planting.getRiceVariety());
            plantingMap.put("sowingDate", planting.getSowingDate());
            plantingMap.put("actualHarvestDate", planting.getActualHarvestDate());
            plantingMap.put("organicCertified", planting.getOrganicCertified());
            plantingMap.put("greenCertified", planting.getGreenCertified());
        }
        return plantingMap;
    }

    private List<Map<String, Object>> farmingLogsOf(String plantingBatchId) {
        List<RiceFarmingLog> logs = farmingLogMapper.selectList(new LambdaQueryWrapper<RiceFarmingLog>()
                .eq(RiceFarmingLog::getPlantingBatchId, plantingBatchId)
                .orderByAsc(RiceFarmingLog::getOperationTime));
        List<Map<String, Object>> list = new ArrayList<>();
        for (RiceFarmingLog log : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("operationType", log.getOperationType());
            item.put("operationTime", log.getOperationTime());
            item.put("operatorName", log.getOperatorName());
            item.put("materialName", log.getMaterialName());
            item.put("materialDosage", log.getMaterialDosage());
            item.put("materialUnit", log.getMaterialUnit());
            item.put("safeIntervalDays", log.getSafeIntervalDays());
            item.put("description", log.getDescription());
            item.put("dataHash", log.getDataHash());
            item.put("chainStatus", log.getChainStatus());
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> storageOf(RiceStorageReceipt storage) {
        if (storage == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("storageReceiptId", storage.getStorageReceiptId());
        map.put("grainGrade", storage.getGrainGrade());
        map.put("storageTime", storage.getStorageTime());
        map.put("moisturePercent", storage.getMoisturePercent());
        map.put("impurityPercent", storage.getImpurityPercent());
        map.put("temperature", storage.getTemperature());
        map.put("humidity", storage.getHumidity());
        map.put("warehouseCode", storage.getWarehouseCode());
        map.put("chainStatus", storage.getChainStatus());
        return map;
    }

    private Map<String, Object> millingOf(RiceMillingBatch milling, RiceStorageReceipt storage) {
        if (milling == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("millingBatchId", milling.getMillingBatchId());
        map.put("processFlow", processFlowOf(milling.getProcessParams()));
        map.put("yieldRate", milling.getYieldRate());
        map.put("qualitySummary", milling.getQualitySummary());
        map.put("processStartTime", milling.getProcessStartTime());
        map.put("processEndTime", milling.getProcessEndTime());
        map.put("chainStatus", milling.getChainStatus());
        // 出厂/入库质检
        if (storage != null) {
            RiceQualityTest test = qualityTestMapper.selectOne(new LambdaQueryWrapper<RiceQualityTest>()
                    .eq(RiceQualityTest::getBusinessType, "STORAGE")
                    .eq(RiceQualityTest::getBusinessId, storage.getStorageReceiptId())
                    .orderByDesc(RiceQualityTest::getCreateTime).last("LIMIT 1"));
            if (test != null) {
                map.put("qualityResult", test.getOverallResult());
                map.put("testAgency", test.getTestAgency());
                map.put("testTime", test.getTestTime());
                map.put("testItems", qualityTestItemMapper.selectList(
                        new LambdaQueryWrapper<RiceQualityTestItem>()
                                .eq(RiceQualityTestItem::getQualityTestId, test.getQualityTestId())));
            }
        }
        return map;
    }

    /** 工艺参数 -> 工艺流程图（脱壳/碾白/抛光/色选/包装） */
    private List<String> processFlowOf(Map<String, Object> processParams) {
        List<String> flow = new ArrayList<>();
        if (processParams == null) {
            return flow;
        }
        if (processParams.containsKey("hulling")) {
            flow.add("脱壳");
        }
        if (processParams.containsKey("polishing")) {
            flow.add("抛光");
        }
        if (processParams.containsKey("colorSorting")) {
            flow.add("色选");
        }
        if (processParams.containsKey("packaging")) {
            flow.add("包装");
        }
        return flow;
    }

    private List<Map<String, Object>> certificationsOf(RiceProductBatch productBatch) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (productBatch == null || productBatch.getCertificationFileIds() == null) {
            return list;
        }
        for (String fileId : productBatch.getCertificationFileIds()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fileId", fileId);
            FileResource resource = fileResourceMapper.selectById(fileId);
            if (resource != null) {
                item.put("name", resource.getFileName());
                item.put("fileUrl", resource.getFileUrl());
            } else {
                item.put("fileUrl", "/api/v1/files/" + fileId + "/download");
            }
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> chainProofMapOf(RiceTraceCode code, RiceMillingBatch milling, RiceStorageReceipt storage) {
        RiceChainProof proof = latestProof(code, milling);
        Map<String, Object> map = new LinkedHashMap<>();
        if (proof == null) {
            map.put("verified", false);
            return map;
        }
        map.put("verified", "SUCCESS".equals(proof.getChainStatus()));
        map.put("businessType", proof.getBusinessType());
        map.put("businessId", proof.getBusinessId());
        map.put("dataHash", proof.getDataHash());
        map.put("txId", proof.getTxId());
        map.put("blockHeight", proof.getBlockHeight());
        map.put("chainTime", proof.getChainTime());
        return map;
    }

    /** 取最相关的存证：激活存证 -> 加工存证 -> 入库存证 */
    private RiceChainProof latestProof(RiceTraceCode code, RiceMillingBatch milling) {
        RiceChainProof proof = chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                .eq(RiceChainProof::getBusinessType, "TRACE_CODE")
                .eq(RiceChainProof::getBusinessId, code.getProductBatchId()));
        if (proof == null && milling != null) {
            proof = chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                    .eq(RiceChainProof::getBusinessType, "MILLING_BATCH")
                    .eq(RiceChainProof::getBusinessId, milling.getMillingBatchId()));
        }
        return proof;
    }

    private boolean isChainVerified(String productBatchId) {
        RiceChainProof proof = chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                .eq(RiceChainProof::getBusinessType, "TRACE_CODE")
                .eq(RiceChainProof::getBusinessId, productBatchId));
        return proof != null && "SUCCESS".equals(proof.getChainStatus());
    }

    private List<String> fileUrlsOf(List<String> fileIds) {
        List<String> urls = new ArrayList<>();
        if (fileIds == null) {
            return urls;
        }
        for (String fileId : fileIds) {
            FileResource resource = fileResourceMapper.selectById(fileId);
            urls.add(resource != null ? resource.getFileUrl() : "/api/v1/files/" + fileId + "/download");
        }
        return urls;
    }

    private void createWarning(String type, String riskLevel, String businessType, String businessId, String content) {
        RiceRiskWarning warning = new RiceRiskWarning();
        warning.setWarningId(IdGen.generate("WARN"));
        warning.setWarningType(type);
        warning.setRiskLevel(riskLevel);
        warning.setBusinessType(businessType);
        warning.setBusinessId(businessId);
        warning.setWarningContent(content);
        warning.setHandled(false);
        warning.setChainStatus("PENDING");
        riskWarningMapper.insert(warning);
        log.info("扫码风控预警: type={}, business={}, content={}", type, businessId, content);
    }
}
