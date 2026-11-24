package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.CollectBatchDTO;
import com.itheima.qukuailian.dto.CollectDTO;
import com.itheima.qukuailian.dto.CollectReviewDTO;
import com.itheima.qukuailian.entity.RiceCollectRecord;
import com.itheima.qukuailian.entity.RicePackageUnit;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceCollectRecordMapper;
import com.itheima.qukuailian.mapper.RicePackageUnitMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.RiceCollectService;
import com.itheima.qukuailian.service.RiceComplianceGuardService;
import com.itheima.qukuailian.utils.IdGen;
import com.itheima.qukuailian.utils.UserContext;
import com.itheima.qukuailian.vo.ComplianceCheckVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 扫码采集实现：
 * <ol>
 *   <li>不做"扫码即改状态"，采集只负责记录 + 校验，真正的业务写入仍走各业务接口，避免两套写入口</li>
 *   <li>幂等键由终端生成（离线场景必备），数据库唯一索引兜底</li>
 *   <li>校验复用 {@link RiceComplianceGuardService}，保证"扫出来的红绿灯"与"提交时的拦截"同一套规则</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiceCollectServiceImpl extends ServiceImpl<RiceCollectRecordMapper, RiceCollectRecord>
        implements RiceCollectService {

    private final RiceTraceCodeMapper traceCodeMapper;
    private final RicePackageUnitMapper packageUnitMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceComplianceGuardService complianceGuardService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceCollectRecord collect(CollectDTO dto, String ip) {
        String source = StringUtils.hasText(dto.getSource()) ? dto.getSource() : "ONLINE";
        if ("MANUAL".equals(source) && !StringUtils.hasText(dto.getReason())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "人工补录必须填写补录原因");
        }

        String idempotencyKey = StringUtils.hasText(dto.getIdempotencyKey())
                ? dto.getIdempotencyKey()
                : buildIdempotencyKey(dto, source);

        // CJ-05：幂等命中直接返回原记录，不重复入库
        RiceCollectRecord existed = lambdaQuery()
                .eq(RiceCollectRecord::getIdempotencyKey, idempotencyKey).one();
        if (existed != null) {
            existed.setDuplicated(true);
            log.info("[扫码采集] 幂等命中: key={}, collectId={}", idempotencyKey, existed.getCollectId());
            return existed;
        }

        RiceCollectRecord record = new RiceCollectRecord();
        record.setCollectId(IdGen.generate("CLT"));
        record.setIdempotencyKey(idempotencyKey);
        record.setCollectCode(dto.getCode().trim());
        record.setAction(dto.getAction());
        record.setTerminalType(StringUtils.hasText(dto.getTerminalType()) ? dto.getTerminalType() : "WEB");
        record.setOperatorName(StringUtils.hasText(dto.getOperatorName()) ? dto.getOperatorName() : currentUser());
        record.setCollectTime(dto.getCollectTime() == null ? LocalDateTime.now() : dto.getCollectTime());
        record.setSource(source);
        record.setReviewReason(StringUtils.hasText(dto.getReason()) ? dto.getReason() : "");
        record.setDuplicated(false);

        // 1. 识别码类型 + 带出业务快照
        ResolveResult resolved = resolveCode(dto.getCode().trim());
        record.setCodeType(resolved.codeType);
        record.setBusinessType(resolved.businessType);
        record.setBusinessId(resolved.businessId);
        record.setSnapshot(resolved.snapshot);

        // 2. 防错校验
        List<ComplianceCheckVO> checks = new ArrayList<>();
        if (!resolved.exists) {
            checks.add(buildFail("CODE_EXISTS", "码有效性校验", "BLOCK",
                    "系统中不存在该码：" + dto.getCode(), dto.getCode()));
        } else {
            checks.addAll(evaluateGuard(resolved.codeType, resolved.guardBusinessId, dto.getAction()));
        }

        String result = "PASS";
        if (checks.stream().anyMatch(c -> !Boolean.TRUE.equals(c.getPassed()) && "BLOCK".equals(c.getLevel()))) {
            result = "BLOCK";
        } else if (checks.stream().anyMatch(c -> !Boolean.TRUE.equals(c.getPassed()))) {
            result = "WARN";
        }
        record.setCheckResult(result);
        record.setCheckMessage(checks.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getPassed()))
                .map(c -> "【" + c.getRuleName() + "】" + c.getMessage())
                .collect(Collectors.joining("；")));

        // 3. 人工补录必须审核（CJ-07）
        record.setStatus("MANUAL".equals(source) ? "PENDING_REVIEW" : "COLLECTED");
        save(record);

        auditLogService.record("COLLECT_RECORD", record.getCollectId(), "CREATE", null, null,
                "扫码采集：" + dto.getAction() + " / " + dto.getCode() + " / 校验=" + result, ip);
        log.info("[扫码采集] code={}, type={}, action={}, result={}, source={}",
                record.getCollectCode(), record.getCodeType(), record.getAction(), result, source);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCollect(CollectBatchDTO dto, String ip) {
        int accepted = 0;
        int duplicated = 0;
        int blocked = 0;
        List<String> errors = new ArrayList<>();
        for (CollectDTO item : dto.getRecords()) {
            try {
                RiceCollectRecord record = collect(item, ip);
                if (Boolean.TRUE.equals(record.getDuplicated())) {
                    duplicated++;
                } else {
                    accepted++;
                    if ("BLOCK".equals(record.getCheckResult())) {
                        blocked++;
                    }
                }
            } catch (Exception e) {
                // 单条失败不影响整批补传（弱网补传最常见的场景）
                errors.add(item.getCode() + "：" + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", dto.getRecords().size());
        result.put("accepted", accepted);
        result.put("duplicated", duplicated);
        result.put("blocked", blocked);
        result.put("failed", errors.size());
        result.put("errors", errors);
        log.info("[离线补传] total={}, accepted={}, duplicated={}, failed={}",
                dto.getRecords().size(), accepted, duplicated, errors.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceCollectRecord review(String collectId, CollectReviewDTO dto, String ip) {
        RiceCollectRecord record = getById(collectId);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "采集记录不存在: " + collectId);
        }
        if (!"PENDING_REVIEW".equals(record.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(),
                    "只有待审核的补录记录可以审核（当前状态：" + record.getStatus() + "）");
        }
        record.setStatus(Boolean.TRUE.equals(dto.getApproved()) ? "APPROVED" : "REJECTED");
        record.setReviewedBy(currentUser());
        record.setReviewedAt(LocalDateTime.now());
        record.setReviewComment(dto.getComment() == null ? "" : dto.getComment());
        updateById(record);
        auditLogService.record("COLLECT_RECORD", collectId, "HANDLE", null, null,
                "补录审核：" + record.getStatus() + " / " + record.getReviewComment(), ip);
        return record;
    }

    @Override
    public IPage<RiceCollectRecord> page(long pageNo, long pageSize, String code, String action,
                                         String terminalType, String source, String status,
                                         String checkResult, String startTime, String endTime) {
        LambdaQueryWrapper<RiceCollectRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(code), RiceCollectRecord::getCollectCode, code)
                .eq(StringUtils.hasText(action), RiceCollectRecord::getAction, action)
                .eq(StringUtils.hasText(terminalType), RiceCollectRecord::getTerminalType, terminalType)
                .eq(StringUtils.hasText(source), RiceCollectRecord::getSource, source)
                .eq(StringUtils.hasText(status), RiceCollectRecord::getStatus, status)
                .eq(StringUtils.hasText(checkResult), RiceCollectRecord::getCheckResult, checkResult)
                .ge(StringUtils.hasText(startTime), RiceCollectRecord::getCollectTime, parseTime(startTime))
                .le(StringUtils.hasText(endTime), RiceCollectRecord::getCollectTime, parseTime(endTime))
                .orderByDesc(RiceCollectRecord::getCollectTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public Map<String, Object> stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayTotal", countBy(w -> w.ge(RiceCollectRecord::getCollectTime, todayStart)));
        stats.put("todayBlocked", countBy(w -> w.ge(RiceCollectRecord::getCollectTime, todayStart)
                .eq(RiceCollectRecord::getCheckResult, "BLOCK")));
        stats.put("pendingReview", countBy(w -> w.eq(RiceCollectRecord::getStatus, "PENDING_REVIEW")));
        stats.put("offlineTotal", countBy(w -> w.eq(RiceCollectRecord::getSource, "OFFLINE")));

        List<Map<String, Object>> actionDist = new ArrayList<>();
        for (String action : List.of("STORE", "MILL", "ACTIVATE", "OUTBOUND", "SHELF", "SELL", "INSPECT")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("action", action);
            item.put("count", countBy(w -> w.eq(RiceCollectRecord::getAction, action)));
            actionDist.add(item);
        }
        stats.put("actionDistribution", actionDist);
        return stats;
    }

    private Long countBy(java.util.function.Consumer<LambdaQueryWrapper<RiceCollectRecord>> consumer) {
        LambdaQueryWrapper<RiceCollectRecord> wrapper = new LambdaQueryWrapper<>();
        consumer.accept(wrapper);
        Long count = baseMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    // ==================================================================
    // 码识别
    // ==================================================================
    private static class ResolveResult {
        boolean exists;
        String codeType = "UNKNOWN";
        String businessType = "";
        String businessId = "";
        /** 用于防错校验的对象 ID（单品码用码本身，批次用批次 ID） */
        String guardBusinessId = "";
        Map<String, Object> snapshot = new HashMap<>();
    }

    private ResolveResult resolveCode(String code) {
        ResolveResult r = new ResolveResult();

        // 1. 单品码
        RiceTraceCode traceCode = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                .eq(RiceTraceCode::getTraceCode, code).last("LIMIT 1"));
        if (traceCode != null) {
            r.exists = true;
            r.codeType = "TRACE_CODE";
            r.businessType = "TRACE_CODE";
            r.businessId = code;
            r.guardBusinessId = code;
            r.snapshot = snapshotOf(traceCode);
            return r;
        }

        // 2. 箱码 / 托盘码
        RicePackageUnit unit = packageUnitMapper.selectById(code);
        if (unit != null) {
            r.exists = true;
            r.codeType = "BOX".equals(unit.getUnitLevel()) ? "BOX_CODE" : "PALLET_CODE";
            r.businessType = "PACKAGE_UNIT";
            r.businessId = code;
            r.guardBusinessId = unit.getProductBatchId();
            r.snapshot.put("unitCode", unit.getUnitCode());
            r.snapshot.put("unitLevel", unit.getUnitLevel());
            r.snapshot.put("productBatchId", unit.getProductBatchId());
            r.snapshot.put("childCount", unit.getChildCount());
            r.snapshot.put("totalQuantity", unit.getTotalQuantity());
            r.snapshot.put("status", unit.getStatus());
            return r;
        }

        // 3. 成品批次
        RiceProductBatch product = productBatchMapper.selectById(code);
        if (product != null) {
            r.exists = true;
            r.codeType = "PRODUCT_BATCH";
            r.businessType = "PRODUCT_BATCH";
            r.businessId = code;
            r.guardBusinessId = code;
            r.snapshot.put("productName", product.getProductName());
            r.snapshot.put("brandName", product.getBrandName());
            r.snapshot.put("packageSpec", product.getPackageSpec());
            r.snapshot.put("status", product.getStatus());
            r.snapshot.put("expectedSaleRegion", product.getExpectedSaleRegion());
            return r;
        }

        // 4. 原粮批次 / 入库单
        RiceStorageReceipt receipt = storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                .and(w -> w.eq(RiceStorageReceipt::getGrainBatchId, code)
                        .or().eq(RiceStorageReceipt::getStorageReceiptId, code))
                .last("LIMIT 1"));
        if (receipt != null) {
            r.exists = true;
            boolean byGrain = code.equals(receipt.getGrainBatchId());
            r.codeType = byGrain ? "GRAIN_BATCH" : "STORAGE_RECEIPT";
            r.businessType = "STORAGE_RECEIPT";
            r.businessId = receipt.getStorageReceiptId();
            r.guardBusinessId = receipt.getGrainBatchId();
            r.snapshot.put("grainBatchId", receipt.getGrainBatchId());
            r.snapshot.put("warehouseCode", receipt.getWarehouseCode());
            r.snapshot.put("wetGrainWeightKg", receipt.getWetGrainWeightKg());
            r.snapshot.put("grainGrade", receipt.getGrainGrade());
            r.snapshot.put("storageTime",
                    receipt.getStorageTime() == null ? "" : receipt.getStorageTime().toString());
            return r;
        }

        // 5. 种植批次
        RicePlantingBatch planting = plantingBatchMapper.selectById(code);
        if (planting != null) {
            r.exists = true;
            r.codeType = "PLANTING_BATCH";
            r.businessType = "PLANTING_BATCH";
            r.businessId = code;
            r.guardBusinessId = code;
            r.snapshot.put("riceVariety", planting.getRiceVariety());
            // 注意：snapshot 会被 MyBatis 的 JacksonTypeHandler 序列化，其 ObjectMapper 未注册
            // JavaTimeModule，直接放 LocalDate/LocalDateTime 会抛 InvalidDefinitionException。
            // 因此这里统一转成字符串，只允许 String/Number/Boolean 进入快照。
            r.snapshot.put("sowingDate", planting.getSowingDate() == null ? "" : planting.getSowingDate().toString());
            r.snapshot.put("status", planting.getStatus());
            return r;
        }
        return r;
    }

    private Map<String, Object> snapshotOf(RiceTraceCode code) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traceCode", code.getTraceCode());
        snapshot.put("productBatchId", code.getProductBatchId());
        snapshot.put("status", code.getStatus());
        snapshot.put("packageSpec", code.getPackageSpec());
        snapshot.put("scanCount", code.getScanCount());
        snapshot.put("expectedSaleRegion", code.getExpectedSaleRegion());
        snapshot.put("lastScanRegion", code.getLastScanRegion());
        snapshot.put("riskLevel", code.getRiskLevel());
        RiceProductBatch product = productBatchMapper.selectById(code.getProductBatchId());
        if (product != null) {
            snapshot.put("productName", product.getProductName());
            snapshot.put("brandName", product.getBrandName());
            snapshot.put("standardNo", product.getStandardNo());
        }
        return snapshot;
    }

    /**
     * 按"码类型 × 业务动作"决定怎么校验：
     * <ul>
     *   <li>类型与动作匹配 → 走防错规则（批次冻结/召回/休药期/质检/码状态）</li>
     *   <li>类型与动作不匹配 → 直接告警（防止扫错料、扫错环节）</li>
     * </ul>
     */
    private List<ComplianceCheckVO> evaluateGuard(String codeType, String businessId, String action) {
        List<ComplianceCheckVO> results = new ArrayList<>();
        boolean isUnit = "BOX_CODE".equals(codeType) || "PALLET_CODE".equals(codeType);

        switch (action) {
            case "STORE" -> {
                if ("PLANTING_BATCH".equals(codeType)) {
                    results.addAll(complianceGuardService.check("PLANTING_BATCH", businessId,
                            RiceComplianceGuardService.ACTION_STORE));
                } else {
                    results.add(mismatch(codeType, action, "入库收储应扫种植批次"));
                }
            }
            case "MILL" -> {
                if ("GRAIN_BATCH".equals(codeType) || "STORAGE_RECEIPT".equals(codeType)) {
                    results.addAll(complianceGuardService.check("GRAIN_BATCH", businessId,
                            RiceComplianceGuardService.ACTION_MILL));
                } else {
                    results.add(mismatch(codeType, action, "碾米加工应扫原粮批次或入库单"));
                }
            }
            case "ACTIVATE" -> {
                if ("PRODUCT_BATCH".equals(codeType) || isUnit) {
                    results.addAll(complianceGuardService.check("PRODUCT_BATCH", businessId,
                            RiceComplianceGuardService.ACTION_ACTIVATE));
                } else {
                    results.add(mismatch(codeType, action, "赋码激活应扫成品批次"));
                }
            }
            case "SELL", "OUTBOUND", "SHELF" -> {
                if ("TRACE_CODE".equals(codeType)) {
                    results.addAll(complianceGuardService.check("TRACE_CODE", businessId,
                            RiceComplianceGuardService.ACTION_SELL));
                } else if (isUnit) {
                    // 箱/托盘出库：按批次维度校验冻结与召回状态
                    results.addAll(complianceGuardService.check("PRODUCT_BATCH", businessId,
                            RiceComplianceGuardService.ACTION_ACTIVATE));
                } else {
                    results.add(mismatch(codeType, action, "出入库/销售应扫单品码、箱码或托盘码"));
                }
            }
            default -> {
                // INSPECT：只记录，不拦截
            }
        }
        return results;
    }

    private ComplianceCheckVO mismatch(String codeType, String action, String expected) {
        return buildFail("CODE_ACTION_MISMATCH", "码与动作匹配校验", "WARN",
                "当前扫到的是 " + codeType + "，与动作 " + action + " 不匹配，" + expected,
                codeType);
    }

    private String buildIdempotencyKey(CollectDTO dto, String source) {
        LocalDateTime time = dto.getCollectTime() == null ? LocalDateTime.now() : dto.getCollectTime();
        return (source + ":" + dto.getAction() + ":" + dto.getCode().trim() + ":"
                + time.withNano(0)).toLowerCase();
    }

    private ComplianceCheckVO buildFail(String code, String name, String level, String message, String businessId) {
        ComplianceCheckVO vo = new ComplianceCheckVO();
        vo.setRuleCode(code);
        vo.setRuleName(name);
        vo.setPassed(false);
        vo.setLevel(level);
        vo.setMessage(message);
        vo.setBusinessId(businessId);
        return vo;
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return value.length() <= 10
                    ? LocalDate.parse(value).atStartOfDay()
                    : LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String currentUser() {
        try {
            return UserContext.getUsername();
        } catch (Exception e) {
            return "system";
        }
    }
}
