package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.RecallApproveDTO;
import com.itheima.qukuailian.dto.RecallCloseDTO;
import com.itheima.qukuailian.dto.RecallCreateDTO;
import com.itheima.qukuailian.dto.RecallItemHandleDTO;
import com.itheima.qukuailian.dto.RecallReleaseDTO;
import com.itheima.qukuailian.entity.RiceChainProof;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceRecallItem;
import com.itheima.qukuailian.entity.RiceRecallTask;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceChainProofMapper;
import com.itheima.qukuailian.mapper.RiceMillingBatchMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceRecallItemMapper;
import com.itheima.qukuailian.mapper.RiceRecallTaskMapper;
import com.itheima.qukuailian.mapper.RiceRiskWarningMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceRecallService;
import com.itheima.qukuailian.service.RiceTraceQueryService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import com.itheima.qukuailian.utils.UserContext;
import com.itheima.qukuailian.vo.TraceImpactVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 召回管理实现：
 * <ol>
 *   <li>影响范围不在前端算，统一由追溯服务联动计算，避免"页面看到 3 个批次、实际冻结 5 个批次"</li>
 *   <li>发布召回 = 冻结动作，批次置 LOCKED、防伪码置 DISABLED，后续入库/加工/激活/销售全部被防错服务拦截</li>
 *   <li>召回指令与关闭结论上链，处置过程写审计日志，满足"责任可查、通知可证、处置可核验"</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiceRecallServiceImpl extends ServiceImpl<RiceRecallTaskMapper, RiceRecallTask>
        implements RiceRecallService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 允许发起召回前预览的范围类型 */
    private static final List<String> SCOPE_TYPES = List.of(
            "TRACE_CODE", "PRODUCT_BATCH", "MILLING_BATCH", "GRAIN_BATCH", "STORAGE_RECEIPT", "PLANTING_BATCH");

    private final RiceRecallItemMapper recallItemMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceTraceCodeMapper traceCodeMapper;
    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceRiskWarningMapper riskWarningMapper;
    private final RiceChainProofMapper chainProofMapper;
    private final RiceTraceQueryService traceQueryService;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    /** 注册 JavaTime 模块，否则含 LocalDateTime 的实体序列化会失败，导致摘要退化为 toString */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // ==================================================================
    // 影响范围预览
    // ==================================================================
    @Override
    public TraceImpactVO previewScope(String businessType, String businessId) {
        validateScope(businessType, businessId);
        return traceQueryService.impact(businessType, businessId);
    }

    private void validateScope(String businessType, String businessId) {
        if (!StringUtils.hasText(businessType) || !StringUtils.hasText(businessId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请指定影响范围起点（类型 + 业务ID）");
        }
        if (!SCOPE_TYPES.contains(businessType.toUpperCase())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(),
                    "不支持的影响范围起点类型: " + businessType + "，可选 " + SCOPE_TYPES);
        }
    }

    // ==================================================================
    // 创建
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallTask create(RecallCreateDTO dto, String ip) {
        String scopeType = StringUtils.hasText(dto.getScopeBusinessType())
                ? dto.getScopeBusinessType().toUpperCase()
                : (StringUtils.hasText(dto.getTriggerBusinessType())
                ? dto.getTriggerBusinessType().toUpperCase() : null);
        String scopeId = StringUtils.hasText(dto.getScopeBusinessId())
                ? dto.getScopeBusinessId() : dto.getTriggerBusinessId();
        validateScope(scopeType, scopeId);

        // 1. 由追溯服务计算影响范围（成品批次 / 防伪码 / 重量 / 区域）
        TraceImpactVO impact = traceQueryService.impact(scopeType, scopeId);

        RiceRecallTask task = new RiceRecallTask();
        task.setRecallTaskId(IdGen.generate("RCL"));
        task.setRecallNo(buildRecallNo());
        task.setTriggerType(dto.getTriggerType());
        task.setTriggerBusinessType(StringUtils.hasText(dto.getTriggerBusinessType())
                ? dto.getTriggerBusinessType().toUpperCase() : scopeType);
        task.setTriggerBusinessId(StringUtils.hasText(dto.getTriggerBusinessId())
                ? dto.getTriggerBusinessId() : scopeId);
        task.setRiskLevel(StringUtils.hasText(dto.getRiskLevel()) ? dto.getRiskLevel() : "MEDIUM");
        task.setRecallReason(dto.getReason());
        task.setAffectedBatchCount(impact.getProductBatches() == null ? 0 : impact.getProductBatches().size());
        task.setAffectedCodeCount(impact.getTraceCodeCount() == null ? 0 : impact.getTraceCodeCount());
        task.setAffectedWeightKg(impact.getWeightKg() == null ? BigDecimal.ZERO : impact.getWeightKg());
        task.setAffectedRegions(joinRegions(impact));
        task.setStatus("DRAFT");
        task.setDeadline(dto.getDeadline());
        task.setInitiatedBy(currentUser());
        task.setInitiatedAt(LocalDateTime.now());
        task.setChainStatus("PENDING");
        save(task);

        // 2. 生成明细：成品批次级 + 防伪码级 + 原粮批次级
        List<RiceRecallItem> items = new ArrayList<>();
        boolean includeCodes = dto.getIncludeTraceCodes() == null || dto.getIncludeTraceCodes();
        int maxCodes = dto.getMaxTraceCodeItems() == null ? 200 : Math.max(0, dto.getMaxTraceCodeItems());

        for (Map<String, Object> product : impact.getProductBatches()) {
            String productBatchId = String.valueOf(product.get("productBatchId"));
            RiceProductBatch batch = productBatchMapper.selectById(productBatchId);
            RiceRecallItem item = newItem(task.getRecallTaskId(), "PRODUCT_BATCH", productBatchId);
            if (batch != null) {
                item.setProductName(batch.getProductName());
                item.setPackageSpec(batch.getPackageSpec());
                item.setExpectedRegion(batch.getExpectedSaleRegion());
            }
            item.setBatchNo(productBatchId);
            item.setQuantity(intOf(product.get("traceCodeCount")));
            item.setWeightKg(riceOutputOf(productBatchId));
            item.setActualRegion(lastScanRegionOf(productBatchId));
            items.add(item);
        }

        if (includeCodes && maxCodes > 0 && impact.getTraceCodeSamples() != null) {
            int codeItems = 0;
            for (String code : impact.getTraceCodeSamples()) {
                if (codeItems >= maxCodes) {
                    break;
                }
                RiceTraceCode traceCode = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                        .eq(RiceTraceCode::getTraceCode, code).last("LIMIT 1"));
                if (traceCode == null) {
                    continue;
                }
                RiceRecallItem item = newItem(task.getRecallTaskId(), "TRACE_CODE", code);
                item.setBatchNo(traceCode.getProductBatchId());
                item.setPackageSpec(traceCode.getPackageSpec());
                item.setExpectedRegion(traceCode.getExpectedSaleRegion());
                item.setActualRegion(traceCode.getLastScanRegion());
                item.setQuantity(1);
                items.add(item);
                codeItems++;
            }
        }

        // 原粮批次级明细：让加工环节也能被冻结拦截（即使成品尚未赋码）
        if (impact.getGrainBatches() != null) {
            for (String grainBatchId : impact.getGrainBatches()) {
                if (!StringUtils.hasText(grainBatchId)) {
                    continue;
                }
                RiceRecallItem item = newItem(task.getRecallTaskId(), "GRAIN_BATCH", grainBatchId);
                item.setBatchNo(grainBatchId);
                item.setQuantity(0);
                items.add(item);
            }
        }

        items.forEach(recallItemMapper::insert);

        auditLogService.record("RECALL_TASK", task.getRecallTaskId(), "CREATE", null, digest(task),
                "创建召回任务：" + dto.getReason(), ip);
        log.info("召回任务创建: recallNo={}, 明细={} 条, 影响成品批次={}, 影响码={}",
                task.getRecallNo(), items.size(), task.getAffectedBatchCount(), task.getAffectedCodeCount());
        task.setItems(items);
        task.setProgress(buildProgress(items));
        return task;
    }

    private RiceRecallItem newItem(String recallTaskId, String itemType, String businessId) {
        RiceRecallItem item = new RiceRecallItem();
        item.setItemId(IdGen.generate("RI"));
        item.setRecallTaskId(recallTaskId);
        item.setItemType(itemType);
        item.setBusinessId(businessId);
        item.setNotifyStatus("PENDING");
        item.setHandleStatus("PENDING");
        item.setFrozen(false);
        item.setQuantity(0);
        item.setWeightKg(BigDecimal.ZERO);
        return item;
    }

    private String buildRecallNo() {
        String date = LocalDateTime.now().format(DATE_FMT);
        long count = lambdaQuery().likeRight(RiceRecallTask::getRecallNo, "RC-" + date).count();
        return "RC-" + date + "-" + String.format("%04d", count + 1);
    }

    private String joinRegions(TraceImpactVO impact) {
        if (impact.getRegions() == null || impact.getRegions().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> region : impact.getRegions()) {
            if (sb.length() > 0) {
                sb.append("，");
            }
            sb.append(region.get("region"));
        }
        String text = sb.toString();
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private BigDecimal riceOutputOf(String productBatchId) {
        List<RiceMillingBatch> millings = millingBatchMapper.selectList(
                new LambdaQueryWrapper<RiceMillingBatch>()
                        .eq(RiceMillingBatch::getProductBatchId, productBatchId));
        return millings.stream().map(RiceMillingBatch::getRiceOutputWeightKg)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String lastScanRegionOf(String productBatchId) {
        List<RiceTraceCode> codes = traceCodeMapper.selectList(new LambdaQueryWrapper<RiceTraceCode>()
                .eq(RiceTraceCode::getProductBatchId, productBatchId)
                .orderByDesc(RiceTraceCode::getLastScanRegion)
                .last("LIMIT 1"));
        return codes.isEmpty() ? "" : codes.get(0).getLastScanRegion();
    }

    // ==================================================================
    // 流程流转
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallTask submit(String recallTaskId, String ip) {
        RiceRecallTask task = mustGet(recallTaskId);
        assertStatus(task, "DRAFT", "只有草稿状态可以提交审批");
        task.setStatus("SUBMITTED");
        updateById(task);
        auditLogService.record("RECALL_TASK", recallTaskId, "UPDATE", null, digest(task), "提交召回审批", ip);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallTask approve(String recallTaskId, RecallApproveDTO dto, String ip) {
        RiceRecallTask task = mustGet(recallTaskId);
        assertStatus(task, "SUBMITTED", "只有已提交状态可以审批");
        task.setStatus("APPROVED");
        task.setApprovedBy(currentUser());
        task.setApprovedAt(LocalDateTime.now());
        task.setApproveComment(dto == null || dto.getComment() == null ? "" : dto.getComment());
        updateById(task);
        auditLogService.record("RECALL_TASK", recallTaskId, "UPDATE", null, digest(task),
                "召回审批通过：" + task.getApproveComment(), ip);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallTask reject(String recallTaskId, RecallApproveDTO dto, String ip) {
        RiceRecallTask task = mustGet(recallTaskId);
        assertStatus(task, "SUBMITTED", "只有已提交状态可以驳回");
        task.setStatus("REJECTED");
        task.setApprovedBy(currentUser());
        task.setApprovedAt(LocalDateTime.now());
        task.setApproveComment(dto == null || dto.getComment() == null ? "" : dto.getComment());
        updateById(task);
        auditLogService.record("RECALL_TASK", recallTaskId, "UPDATE", null, digest(task),
                "召回审批驳回：" + task.getApproveComment(), ip);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallTask release(String recallTaskId, RecallReleaseDTO dto, String ip) {
        RiceRecallTask task = mustGet(recallTaskId);
        assertStatus(task, "APPROVED", "只有审批通过的任务可以发布");

        List<RiceRecallItem> items = listItems(recallTaskId);
        if (items.isEmpty()) {
            throw new BizException(ResultCode.VALIDATION_FAILED.getCode(), "召回任务没有明细，无法发布");
        }
        String channels = dto == null || dto.getNotifyChannels() == null
                ? "SMS" : String.join(",", dto.getNotifyChannels());
        LocalDateTime now = LocalDateTime.now();

        int frozenBatches = 0;
        int frozenCodes = 0;
        int frozenGrain = 0;
        for (RiceRecallItem item : items) {
            // 1. 明细置为已通知 + 已冻结
            item.setFrozen(true);
            item.setNotifyStatus("SENT");
            item.setNotifyChannel(channels);
            item.setNotifiedAt(now);
            recallItemMapper.updateById(item);

            // 2. 按类型执行冻结（这是"销售拦截"的落地动作）
            switch (item.getItemType()) {
                case "PRODUCT_BATCH" -> {
                    RiceProductBatch product = productBatchMapper.selectById(item.getBusinessId());
                    if (product != null && !"LOCKED".equals(product.getStatus())) {
                        product.setStatus("LOCKED");
                        productBatchMapper.updateById(product);
                        frozenBatches++;
                    }
                }
                case "TRACE_CODE" -> {
                    RiceTraceCode code = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                            .eq(RiceTraceCode::getTraceCode, item.getBusinessId()).last("LIMIT 1"));
                    if (code != null && !"DISABLED".equals(code.getStatus())) {
                        code.setStatus("DISABLED");
                        code.setRiskLevel("HIGH");
                        traceCodeMapper.updateById(code);
                        frozenCodes++;
                    }
                }
                case "GRAIN_BATCH" -> frozenGrain += freezePlantingByGrain(item.getBusinessId());
                default -> log.warn("未知召回明细类型: {}", item.getItemType());
            }
        }

        task.setStatus("RELEASED");
        task.setReleasedAt(now);
        if (dto != null && dto.getDeadline() != null) {
            task.setDeadline(dto.getDeadline());
        }
        task.setChainStatus("PENDING");
        updateById(task);

        // 3. 生成召回预警（进入监管待办）
        createRecallWarning(task, "已发布召回 " + task.getRecallNo() + "，冻结批次 " + frozenBatches
                + " 个、防伪码 " + frozenCodes + " 枚、原粮批次 " + frozenGrain + " 个");

        // 4. 召回指令上链（不可抵赖）
        chainProofService.submit("RECALL", recallTaskId, digest(task), null);
        auditLogService.record("RECALL_TASK", recallTaskId, "UPDATE", null, digest(task),
                "发布召回并冻结相关批次/防伪码（批次 " + frozenBatches + "、码 " + frozenCodes + "）", ip);
        log.info("召回发布: recallNo={}, 冻结批次={}, 冻结码={}, 冻结原粮={}",
                task.getRecallNo(), frozenBatches, frozenCodes, frozenGrain);
        task.setItems(items);
        task.setProgress(buildProgress(items));
        return task;
    }

    private int freezePlantingByGrain(String grainBatchId) {
        RiceStorageReceipt receipt = storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                .eq(RiceStorageReceipt::getGrainBatchId, grainBatchId)
                .orderByDesc(RiceStorageReceipt::getStorageTime)
                .last("LIMIT 1"));
        if (receipt == null) {
            return 0;
        }
        RicePlantingBatch batch = plantingBatchMapper.selectById(receipt.getPlantingBatchId());
        if (batch == null || "LOCKED".equals(batch.getStatus())) {
            return 0;
        }
        batch.setStatus("LOCKED");
        plantingBatchMapper.updateById(batch);
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallItem handleItem(String itemId, RecallItemHandleDTO dto, String ip) {
        RiceRecallItem item = recallItemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "召回明细不存在: " + itemId);
        }
        RiceRecallTask task = mustGet(item.getRecallTaskId());
        if (!List.of("RELEASED", "HANDLING").contains(task.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "召回任务尚未发布，无法登记处置结果");
        }

        item.setHandleStatus(dto.getHandleStatus());
        item.setHandleRemark(dto.getRemark() == null ? "" : dto.getRemark());
        item.setHandledBy(currentUser());
        item.setHandledAt(LocalDateTime.now());
        recallItemMapper.updateById(item);

        if ("RELEASED".equals(task.getStatus())) {
            task.setStatus("HANDLING");
            updateById(task);
        }
        auditLogService.record("RECALL_ITEM", itemId, "HANDLE", null, digest(item),
                "召回明细处置为 " + dto.getHandleStatus() + "：" + item.getHandleRemark(), ip);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallTask close(String recallTaskId, RecallCloseDTO dto, String ip) {
        RiceRecallTask task = mustGet(recallTaskId);
        if (!List.of("RELEASED", "HANDLING").contains(task.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "只有已发布/处置中的召回任务可以关闭");
        }
        List<RiceRecallItem> items = listItems(recallTaskId);
        long unRecovered = items.stream()
                .filter(i -> !List.of("RECOVERED", "SCRAPPED", "RETURNED", "ISOLATED").contains(i.getHandleStatus()))
                .count();

        task.setStatus("CLOSED");
        task.setClosedAt(LocalDateTime.now());
        task.setCloseResult(dto.getCloseResult());
        task.setChainStatus("PENDING");
        updateById(task);

        chainProofService.submit("RECALL", recallTaskId, digest(task), null);
        auditLogService.record("RECALL_TASK", recallTaskId, "UPDATE", null, digest(task),
                "关闭召回：" + dto.getCloseResult() + "（未完成处置 " + unRecovered + " 条）", ip);

        createRecallWarning(task, "召回任务 " + task.getRecallNo() + " 已关闭，未完成处置 "
                + unRecovered + " 条，结论：" + dto.getCloseResult());
        task.setItems(items);
        task.setProgress(buildProgress(items));
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceRecallTask cancel(String recallTaskId, String reason, String ip) {
        RiceRecallTask task = mustGet(recallTaskId);
        if (List.of("RELEASED", "HANDLING", "CLOSED", "CANCELLED").contains(task.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "已发布或已关闭的召回任务不能取消");
        }
        task.setStatus("CANCELLED");
        task.setCloseResult(StringUtils.hasText(reason) ? reason : "发起方取消");
        updateById(task);
        auditLogService.record("RECALL_TASK", recallTaskId, "UPDATE", null, digest(task),
                "取消召回：" + task.getCloseResult(), ip);
        return task;
    }

    // ==================================================================
    // 查询
    // ==================================================================
    @Override
    public IPage<RiceRecallTask> page(long pageNo, long pageSize, String status, String riskLevel,
                                      String triggerType, String keyword, String startTime, String endTime) {
        LambdaQueryWrapper<RiceRecallTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), RiceRecallTask::getStatus, status)
                .eq(StringUtils.hasText(riskLevel), RiceRecallTask::getRiskLevel, riskLevel)
                .eq(StringUtils.hasText(triggerType), RiceRecallTask::getTriggerType, triggerType)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(RiceRecallTask::getRecallNo, keyword)
                        .or().like(RiceRecallTask::getRecallReason, keyword)
                        .or().like(RiceRecallTask::getTriggerBusinessId, keyword))
                .ge(StringUtils.hasText(startTime), RiceRecallTask::getCreateTime, parseTime(startTime))
                .le(StringUtils.hasText(endTime), RiceRecallTask::getCreateTime, parseTime(endTime))
                .orderByDesc(RiceRecallTask::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public RiceRecallTask detail(String recallTaskId) {
        RiceRecallTask task = mustGet(recallTaskId);
        List<RiceRecallItem> items = listItems(recallTaskId);
        task.setItems(items);
        task.setProgress(buildProgress(items));

        Map<String, Object> triggerDetail = new LinkedHashMap<>();
        triggerDetail.put("triggerBusinessType", task.getTriggerBusinessType());
        triggerDetail.put("triggerBusinessId", task.getTriggerBusinessId());
        if ("PRODUCT_BATCH".equals(task.getTriggerBusinessType())) {
            RiceProductBatch product = productBatchMapper.selectById(task.getTriggerBusinessId());
            if (product != null) {
                triggerDetail.put("productName", product.getProductName());
                triggerDetail.put("brandName", product.getBrandName());
                triggerDetail.put("status", product.getStatus());
            }
        }
        task.setTriggerDetail(triggerDetail);

        // 补链上信息（上链是异步的，列表/详情读取最新值）
        RiceChainProof proof = chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                .eq(RiceChainProof::getBusinessType, "RECALL")
                .eq(RiceChainProof::getBusinessId, recallTaskId)
                .last("LIMIT 1"));
        if (proof != null) {
            task.setChainStatus(proof.getChainStatus());
            task.setTxId(proof.getTxId());
            task.setBlockHeight(proof.getBlockHeight());
            task.setChainTime(proof.getChainTime());
            task.setChainError(proof.getChainError());
        }
        return task;
    }

    // ==================================================================
    // 工具
    // ==================================================================
    private RiceRecallTask mustGet(String recallTaskId) {
        RiceRecallTask task = getById(recallTaskId);
        if (task == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "召回任务不存在: " + recallTaskId);
        }
        return task;
    }

    private void assertStatus(RiceRecallTask task, String expected, String message) {
        if (!expected.equals(task.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(),
                    message + "（当前状态：" + task.getStatus() + "）");
        }
    }

    private List<RiceRecallItem> listItems(String recallTaskId) {
        return recallItemMapper.selectList(new LambdaQueryWrapper<RiceRecallItem>()
                .eq(RiceRecallItem::getRecallTaskId, recallTaskId)
                .orderByAsc(RiceRecallItem::getItemType)
                .orderByAsc(RiceRecallItem::getCreateTime));
    }

    /** 处置进度：召回绩效指标（回收率/送达率/未召回数） */
    private Map<String, Object> buildProgress(List<RiceRecallItem> items) {
        Map<String, Object> progress = new LinkedHashMap<>();
        int total = items.size();
        int notified = (int) items.stream().filter(i -> !"PENDING".equals(i.getNotifyStatus())).count();
        int confirmed = (int) items.stream().filter(i -> "CONFIRMED".equals(i.getNotifyStatus())).count();
        int offShelf = count(items, "OFF_SHELF");
        int recovered = count(items, "RECOVERED");
        int isolated = count(items, "ISOLATED");
        int returned = count(items, "RETURNED");
        int scrapped = count(items, "SCRAPPED");
        int unRecovered = count(items, "UNRECOVERED") + count(items, "PENDING");
        int settled = recovered + isolated + returned + scrapped;

        progress.put("total", total);
        progress.put("notifiedCount", notified);
        progress.put("confirmedCount", confirmed);
        progress.put("offShelfCount", offShelf);
        progress.put("recoveredCount", recovered);
        progress.put("isolatedCount", isolated);
        progress.put("returnedCount", returned);
        progress.put("scrappedCount", scrapped);
        progress.put("unRecoveredCount", unRecovered);
        progress.put("notifyRate", total == 0 ? 0 : notified * 100 / total);
        progress.put("recoveryRate", total == 0 ? 0 : settled * 100 / total);
        progress.put("frozenCount", (int) items.stream().filter(i -> Boolean.TRUE.equals(i.getFrozen())).count());
        return progress;
    }

    private int count(List<RiceRecallItem> items, String status) {
        return (int) items.stream().filter(i -> status.equals(i.getHandleStatus())).count();
    }

    private void createRecallWarning(RiceRecallTask task, String content) {
        RiceRiskWarning warning = new RiceRiskWarning();
        warning.setWarningId(IdGen.generate("WARN"));
        warning.setWarningType("RECALL_TRIGGERED");
        warning.setRiskLevel(task.getRiskLevel());
        warning.setBusinessType("RECALL_TASK");
        warning.setBusinessId(task.getRecallTaskId());
        warning.setWarningContent(content);
        warning.setHandled(false);
        warning.setChainStatus("PENDING");
        riskWarningMapper.insert(warning);
    }

    private int intOf(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            // 兼容前端传 "2026-11-02"（date）与 "2026-11-02T10:00:00"（datetime）
            return value.length() <= 10
                    ? java.time.LocalDate.parse(value).atStartOfDay()
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

    private String digest(Object obj) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(obj));
        }
    }
}
