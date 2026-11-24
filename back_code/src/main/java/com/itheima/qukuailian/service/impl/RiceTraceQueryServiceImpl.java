package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.entity.RiceChainProof;
import com.itheima.qukuailian.entity.RiceFarmingLog;
import com.itheima.qukuailian.entity.RiceField;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RicePackageUnit;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceQualityTest;
import com.itheima.qukuailian.entity.RiceRecallItem;
import com.itheima.qukuailian.entity.RiceScanLog;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceChainProofMapper;
import com.itheima.qukuailian.mapper.RiceFarmingLogMapper;
import com.itheima.qukuailian.mapper.RiceFieldMapper;
import com.itheima.qukuailian.mapper.RiceMillingBatchMapper;
import com.itheima.qukuailian.mapper.RicePackageUnitMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceQualityTestMapper;
import com.itheima.qukuailian.mapper.RiceRecallItemMapper;
import com.itheima.qukuailian.mapper.RiceScanLogMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.RiceTraceQueryService;
import com.itheima.qukuailian.vo.TraceChainEdgeVO;
import com.itheima.qukuailian.vo.TraceChainNodeVO;
import com.itheima.qukuailian.vo.TraceChainVO;
import com.itheima.qukuailian.vo.TraceImpactVO;
import com.itheima.qukuailian.vo.TraceResolveVO;
import com.itheima.qukuailian.vo.TraceTimelineItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 追溯查询实现：以"一次业务链路"为单位组织数据，不做多表 JOIN，全部走主键/索引查询，
 * 保证在 Demo 数据量下秒级返回，也便于后续替换为图数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiceTraceQueryServiceImpl implements RiceTraceQueryService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 单次链路最多展开的成品批次数（防止一个地块下的海量批次撑爆响应） */
    private static final int MAX_PRODUCTS = 30;

    /** 防伪码统计最多取样的条数 */
    private static final int MAX_CODE_SAMPLE = 500;

    /** 处于召回处置中的明细状态（这些状态下该码/批次禁止流通） */
    private static final List<String> RECALL_ACTIVE_STATUS = List.of(
            "PENDING", "OFF_SHELF", "RECOVERED", "ISOLATED", "UNRECOVERED");

    private static final Map<String, String> OPERATION_LABEL = Map.of(
            "SOWING", "播种",
            "FERTILIZATION", "施肥",
            "FERTILIZER", "施肥",
            "PESTICIDE", "用药",
            "IRRIGATION", "灌溉",
            "WEEDING", "除草",
            "HARVEST", "收割",
            "HARVESTING", "收割",
            "OTHER", "其他农事");

    private final RiceFieldMapper fieldMapper;
    private final RicePlantingBatchMapper plantingBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceTraceCodeMapper traceCodeMapper;
    private final RiceScanLogMapper scanLogMapper;
    private final RiceFarmingLogMapper farmingLogMapper;
    private final RiceQualityTestMapper qualityTestMapper;
    private final RiceChainProofMapper chainProofMapper;
    private final RiceRecallItemMapper recallItemMapper;
    private final RicePackageUnitMapper packageUnitMapper;

    // ==================================================================
    // 1. 入口识别
    // ==================================================================
    @Override
    public TraceResolveVO resolve(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请输入单品码或批次号");
        }
        String key = keyword.trim();

        // 1.1 单品防伪码（消费者扫码 / 扫码枪扫包装）
        RiceTraceCode code = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                .eq(RiceTraceCode::getTraceCode, key).last("LIMIT 1"));
        if (code != null) {
            RiceProductBatch product = productBatchMapper.selectById(code.getProductBatchId());
            TraceResolveVO vo = new TraceResolveVO();
            vo.setKeyword(key);
            vo.setMatchedType("TRACE_CODE");
            vo.setMatchedId(code.getTraceCode());
            vo.setLabel("单品码 " + code.getTraceCode());
            vo.setNote(product == null ? "所属成品批次已不存在"
                    : "所属成品批次 " + product.getProductName() + "（" + code.getProductBatchId() + "）");
            return vo;
        }

        // 1.2 箱码 / 托盘码（YM-01：上层包装码也要能追溯）
        RicePackageUnit unit = packageUnitMapper.selectById(key);
        if (unit != null) {
            TraceResolveVO vo = new TraceResolveVO();
            vo.setKeyword(key);
            vo.setMatchedType("BOX".equals(unit.getUnitLevel()) ? "BOX_CODE" : "PALLET_CODE");
            vo.setMatchedId(unit.getUnitCode());
            vo.setLabel(("BOX".equals(unit.getUnitLevel()) ? "箱码 " : "托盘码 ") + unit.getUnitCode());
            vo.setNote("含 " + unit.getChildCount() + " 个" + ("BOX".equals(unit.getUnitLevel()) ? "单品码" : "箱码")
                    + "，折算单品 " + unit.getTotalQuantity() + " 件，所属成品批次 " + unit.getProductBatchId());
            return vo;
        }

        // 1.3 成品米批次
        RiceProductBatch product = productBatchMapper.selectById(key);
        if (product != null) {
            return simpleResolve(key, "PRODUCT_BATCH", key,
                    "成品批次 " + product.getProductName(),
                    product.getBrandName() + " / " + product.getPackageSpec());
        }

        // 1.3 加工批次
        RiceMillingBatch milling = millingBatchMapper.selectById(key);
        if (milling != null) {
            return simpleResolve(key, "MILLING_BATCH", key,
                    "加工批次 " + milling.getMillingBatchId(),
                    "原粮批次 " + milling.getGrainBatchId());
        }

        // 1.4 入库单
        RiceStorageReceipt receipt = storageReceiptMapper.selectById(key);
        if (receipt != null) {
            return simpleResolve(key, "STORAGE_RECEIPT", key,
                    "入库单 " + receipt.getStorageReceiptId(),
                    "原粮批次 " + receipt.getGrainBatchId());
        }

        // 1.5 原粮批次（可能对应多条入库单，取最新一条）
        receipt = storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                .eq(RiceStorageReceipt::getGrainBatchId, key)
                .orderByDesc(RiceStorageReceipt::getStorageTime)
                .last("LIMIT 1"));
        if (receipt != null) {
            return simpleResolve(key, "GRAIN_BATCH", receipt.getGrainBatchId(),
                    "原粮批次 " + receipt.getGrainBatchId(),
                    "入仓编号 " + receipt.getWarehouseCode());
        }

        // 1.6 种植批次
        RicePlantingBatch planting = plantingBatchMapper.selectById(key);
        if (planting != null) {
            return simpleResolve(key, "PLANTING_BATCH", key,
                    "种植批次 " + planting.getPlantingBatchId(),
                    "水稻品种 " + planting.getRiceVariety());
        }

        // 1.7 地块
        RiceField field = fieldMapper.selectById(key);
        if (field != null) {
            return simpleResolve(key, "FIELD", key, "地块 " + field.getFieldName(),
                    field.getProvince() + field.getCity() + field.getDistrict());
        }

        throw new BizException(ResultCode.NOT_FOUND.getCode(),
                "未匹配到任何业务对象，请检查单品码/批次号是否正确：" + key);
    }

    private TraceResolveVO simpleResolve(String keyword, String type, String id, String label, String note) {
        TraceResolveVO vo = new TraceResolveVO();
        vo.setKeyword(keyword);
        vo.setMatchedType(type);
        vo.setMatchedId(id);
        vo.setLabel(label);
        vo.setNote(note);
        return vo;
    }

    // ==================================================================
    // 2. 链路构建
    // ==================================================================
    @Override
    public TraceChainVO chain(String businessType, String businessId, String direction) {
        ChainContext ctx = loadContext(businessType, businessId);
        TraceChainVO vo = new TraceChainVO();
        vo.setRootType(businessType);
        vo.setRootId(businessId);
        vo.setRootLabel(rootLabel(ctx, businessType, businessId));
        vo.setDirection(StringUtils.hasText(direction) ? direction : "BOTH");
        vo.setNodes(new ArrayList<>());
        vo.setEdges(new ArrayList<>());
        vo.setTimeline(new ArrayList<>());
        vo.setTips(new ArrayList<>());

        buildNodes(ctx, vo);
        buildEdges(ctx, vo);
        buildTimeline(ctx, vo);
        fillSummary(ctx, vo);
        fillChainProofs(vo);
        return vo;
    }

    /** 链路上下文：一次查询把纵向实体全部捞出来，供节点/时间轴/汇总复用 */
    private static class ChainContext {
        RiceField field;
        RicePlantingBatch planting;
        List<RiceStorageReceipt> receipts = new ArrayList<>();
        List<RiceMillingBatch> millings = new ArrayList<>();
        List<RiceProductBatch> products = new ArrayList<>();
        List<RiceFarmingLog> farmingLogs = new ArrayList<>();
        List<RiceQualityTest> qualityTests = new ArrayList<>();
        List<RiceTraceCode> codeSamples = new ArrayList<>();
        int codeCount;
        int scanCount;
        int scannedCodeCount;
        int frozenCodeCount;
        int environmentCount;
    }

    private ChainContext loadContext(String businessType, String businessId) {
        ChainContext ctx = new ChainContext();
        String type = businessType == null ? "" : businessType.toUpperCase();

        switch (type) {
            case "TRACE_CODE" -> {
                RiceTraceCode code = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                        .eq(RiceTraceCode::getTraceCode, businessId).last("LIMIT 1"));
                if (code == null) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "防伪码不存在: " + businessId);
                }
                addProduct(ctx, code.getProductBatchId());
            }
            case "PRODUCT_BATCH" -> addProduct(ctx, businessId);
            case "BOX_CODE", "PALLET_CODE" -> {
                RicePackageUnit unit = packageUnitMapper.selectById(businessId);
                if (unit == null) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "箱码/托盘码不存在: " + businessId);
                }
                addProduct(ctx, unit.getProductBatchId());
            }
            case "MILLING_BATCH" -> {
                RiceMillingBatch milling = millingBatchMapper.selectById(businessId);
                if (milling == null) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "加工批次不存在: " + businessId);
                }
                ctx.millings.add(milling);
                addProduct(ctx, milling.getProductBatchId());
            }
            case "STORAGE_RECEIPT" -> {
                RiceStorageReceipt receipt = storageReceiptMapper.selectById(businessId);
                if (receipt == null) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "入库单不存在: " + businessId);
                }
                ctx.receipts.add(receipt);
            }
            case "GRAIN_BATCH" -> ctx.receipts.addAll(listReceiptsByGrain(businessId));
            case "PLANTING_BATCH" -> {
                ctx.planting = plantingBatchMapper.selectById(businessId);
                if (ctx.planting == null) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "种植批次不存在: " + businessId);
                }
            }
            case "FIELD" -> {
                ctx.field = fieldMapper.selectById(businessId);
                if (ctx.field == null) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "地块不存在: " + businessId);
                }
                List<RicePlantingBatch> batches = plantingBatchMapper.selectList(
                        new LambdaQueryWrapper<RicePlantingBatch>()
                                .eq(RicePlantingBatch::getFieldId, businessId)
                                .orderByDesc(RicePlantingBatch::getSowingDate)
                                .last("LIMIT 1"));
                if (!batches.isEmpty()) {
                    ctx.planting = batches.get(0);
                }
            }
            default -> throw new BizException(ResultCode.PARAM_ERROR.getCode(),
                    "不支持的追溯类型: " + businessType);
        }

        // ---- 纵向补全：任何一个入口都能把上下游补齐 ----
        // 成品 -> 加工
        if (ctx.millings.isEmpty() && !ctx.products.isEmpty()) {
            List<String> productIds = ctx.products.stream().map(RiceProductBatch::getProductBatchId).toList();
            ctx.millings.addAll(millingBatchMapper.selectList(new LambdaQueryWrapper<RiceMillingBatch>()
                    .in(RiceMillingBatch::getProductBatchId, productIds)
                    .orderByAsc(RiceMillingBatch::getProcessStartTime)));
        }
        // 加工 -> 入库
        if (ctx.receipts.isEmpty() && !ctx.millings.isEmpty()) {
            List<String> grainIds = ctx.millings.stream().map(RiceMillingBatch::getGrainBatchId)
                    .filter(StringUtils::hasText).distinct().toList();
            if (!grainIds.isEmpty()) {
                ctx.receipts.addAll(storageReceiptMapper.selectList(new LambdaQueryWrapper<RiceStorageReceipt>()
                        .in(RiceStorageReceipt::getGrainBatchId, grainIds)
                        .orderByAsc(RiceStorageReceipt::getStorageTime)));
            }
        }
        // 入库 -> 种植 -> 地块
        if (ctx.planting == null && !ctx.receipts.isEmpty()) {
            ctx.planting = plantingBatchMapper.selectById(ctx.receipts.get(0).getPlantingBatchId());
        }
        if (ctx.field == null && ctx.planting != null) {
            ctx.field = fieldMapper.selectById(ctx.planting.getFieldId());
        }
        // 种植 -> 入库（正向）
        if (ctx.receipts.isEmpty() && ctx.planting != null) {
            ctx.receipts.addAll(listReceiptsByPlanting(ctx.planting.getPlantingBatchId()));
        }
        // 入库 -> 加工（正向）
        if (ctx.millings.isEmpty() && !ctx.receipts.isEmpty()) {
            List<String> grainIds = ctx.receipts.stream().map(RiceStorageReceipt::getGrainBatchId)
                    .filter(StringUtils::hasText).distinct().toList();
            if (!grainIds.isEmpty()) {
                ctx.millings.addAll(millingBatchMapper.selectList(new LambdaQueryWrapper<RiceMillingBatch>()
                        .in(RiceMillingBatch::getGrainBatchId, grainIds)
                        .orderByAsc(RiceMillingBatch::getProcessStartTime)));
            }
        }
        // 加工 -> 成品（正向）
        if (ctx.products.isEmpty() && !ctx.millings.isEmpty()) {
            List<String> productIds = ctx.millings.stream().map(RiceMillingBatch::getProductBatchId)
                    .filter(StringUtils::hasText).distinct().limit(MAX_PRODUCTS).toList();
            for (String pid : productIds) {
                addProduct(ctx, pid);
            }
        }

        // ---- 农事记录 / 质检记录 ----
        if (ctx.planting != null) {
            ctx.farmingLogs = farmingLogMapper.selectList(new LambdaQueryWrapper<RiceFarmingLog>()
                    .eq(RiceFarmingLog::getPlantingBatchId, ctx.planting.getPlantingBatchId())
                    .orderByAsc(RiceFarmingLog::getOperationTime));
        }
        List<String> testBusinessIds = new ArrayList<>();
        ctx.receipts.forEach(r -> testBusinessIds.add(r.getStorageReceiptId()));
        ctx.millings.forEach(m -> testBusinessIds.add(m.getMillingBatchId()));
        if (!testBusinessIds.isEmpty()) {
            ctx.qualityTests = qualityTestMapper.selectList(new LambdaQueryWrapper<RiceQualityTest>()
                    .in(RiceQualityTest::getBusinessId, testBusinessIds)
                    .orderByAsc(RiceQualityTest::getTestTime));
        }

        // ---- 防伪码统计 ----
        List<String> productIds = ctx.products.stream().map(RiceProductBatch::getProductBatchId).toList();
        if (!productIds.isEmpty()) {
            Long total = traceCodeMapper.selectCount(new LambdaQueryWrapper<RiceTraceCode>()
                    .in(RiceTraceCode::getProductBatchId, productIds));
            ctx.codeCount = total == null ? 0 : total.intValue();
            ctx.codeSamples = traceCodeMapper.selectList(new LambdaQueryWrapper<RiceTraceCode>()
                    .in(RiceTraceCode::getProductBatchId, productIds)
                    .orderByDesc(RiceTraceCode::getCreateTime)
                    .last("LIMIT " + MAX_CODE_SAMPLE));
            for (RiceTraceCode c : ctx.codeSamples) {
                if (c.getScanCount() != null) {
                    ctx.scanCount += c.getScanCount();
                    if (c.getScanCount() > 0) {
                        ctx.scannedCodeCount++;
                    }
                }
                if (c.getStatus() != null && !"ACTIVATED".equals(c.getStatus())) {
                    ctx.frozenCodeCount++;
                }
            }
        }
        return ctx;
    }

    private void addProduct(ChainContext ctx, String productBatchId) {
        if (!StringUtils.hasText(productBatchId) || ctx.products.size() >= MAX_PRODUCTS) {
            return;
        }
        boolean exists = ctx.products.stream()
                .anyMatch(p -> productBatchId.equals(p.getProductBatchId()));
        if (exists) {
            return;
        }
        RiceProductBatch product = productBatchMapper.selectById(productBatchId);
        if (product != null) {
            ctx.products.add(product);
        }
    }

    private List<RiceStorageReceipt> listReceiptsByGrain(String grainBatchId) {
        return storageReceiptMapper.selectList(new LambdaQueryWrapper<RiceStorageReceipt>()
                .eq(RiceStorageReceipt::getGrainBatchId, grainBatchId)
                .orderByAsc(RiceStorageReceipt::getStorageTime));
    }

    private List<RiceStorageReceipt> listReceiptsByPlanting(String plantingBatchId) {
        return storageReceiptMapper.selectList(new LambdaQueryWrapper<RiceStorageReceipt>()
                .eq(RiceStorageReceipt::getPlantingBatchId, plantingBatchId)
                .orderByAsc(RiceStorageReceipt::getStorageTime));
    }

    private String rootLabel(ChainContext ctx, String businessType, String businessId) {
        if ("TRACE_CODE".equalsIgnoreCase(businessType)) {
            return "单品码 " + businessId;
        }
        if (!ctx.products.isEmpty() && "PRODUCT_BATCH".equalsIgnoreCase(businessType)) {
            return "成品批次 " + ctx.products.get(0).getProductName();
        }
        if (ctx.planting != null) {
            return "种植批次 " + ctx.planting.getPlantingBatchId() + "（" + ctx.planting.getRiceVariety() + "）";
        }
        return businessType + " " + businessId;
    }

    // ------------------------------------------------------------------
    // 2.1 节点
    // ------------------------------------------------------------------
    private void buildNodes(ChainContext ctx, TraceChainVO vo) {
        List<TraceChainNodeVO> nodes = vo.getNodes();

        // 地块
        if (ctx.field != null) {
            RiceField f = ctx.field;
            TraceChainNodeVO node = newNode("FIELD", f.getFieldId(), "种植", "种植基地", "FIELD",
                    f.getFieldId(), f.getCreateTime(), f.getFarmerName(),
                    joinRegion(f), "地块 " + f.getFieldName() + "，面积 " + nz(f.getAreaMu()) + " 亩");
            node.setMetrics(metrics(
                    "地块编号", f.getFieldCode(),
                    "面积(亩)", f.getAreaMu(),
                    "土壤类型", f.getSoilType(),
                    "种植户", f.getFarmerName(),
                    "坐标点数", f.getGisBoundary() == null ? 0 : f.getGisBoundary().size()));
            node.setFileIds(f.getBasePhotoFileIds());
            node.setChainStatus(f.getChainStatus());
            node.setTxId(f.getTxId());
            node.setDetailRoute("/rice/fields/detail/" + f.getFieldId());
            nodes.add(node);
        }

        // 种植批次
        if (ctx.planting != null) {
            RicePlantingBatch p = ctx.planting;
            TraceChainNodeVO node = newNode("PLANTING_BATCH", p.getPlantingBatchId(), "种植", "种植批次", "PLANTING_BATCH",
                    p.getPlantingBatchId(), atStartOfDay(p.getSowingDate()), ctx.field == null ? "" : ctx.field.getFarmerName(),
                    ctx.field == null ? "" : joinRegion(ctx.field),
                    "品种 " + nz(p.getRiceVariety()) + "，播种 " + nz(p.getSowingDate())
                            + (p.getActualHarvestDate() == null ? "，尚未收割" : "，收割 " + p.getActualHarvestDate()));
            node.setStatus(p.getStatus());
            node.setMetrics(metrics(
                    "品种", p.getRiceVariety(),
                    "种子来源", p.getSeedSource(),
                    "种子批次", p.getSeedBatchNo(),
                    "播种日期", p.getSowingDate(),
                    "实际收割", p.getActualHarvestDate(),
                    "有机认证", Boolean.TRUE.equals(p.getOrganicCertified()) ? "是" : "否",
                    "绿色认证", Boolean.TRUE.equals(p.getGreenCertified()) ? "是" : "否",
                    "农事记录", ctx.farmingLogs.size() + " 条"));
            node.setChainStatus(p.getChainStatus());
            node.setTxId(p.getTxId());
            node.setDetailRoute("/rice/planting-batches/detail/" + p.getPlantingBatchId());
            node.setSummary(node.getSummary() + "；农事记录 " + ctx.farmingLogs.size() + " 条");
            nodes.add(node);
        }

        // 收储入库
        for (RiceStorageReceipt r : ctx.receipts) {
            TraceChainNodeVO node = newNode("STORAGE_RECEIPT", r.getStorageReceiptId(), "收储", "收储入库", "STORAGE_RECEIPT",
                    r.getStorageReceiptId(), r.getStorageTime(), "仓库 " + nz(r.getWarehouseId()),
                    "入仓编号 " + nz(r.getWarehouseCode()),
                    "湿谷 " + nz(r.getWetGrainWeightKg()) + "kg，水分 " + nz(r.getMoisturePercent())
                            + "%，杂质 " + nz(r.getImpurityPercent()) + "%，等级 " + nz(r.getGrainGrade()));
            node.setMetrics(metrics(
                    "原粮批次", r.getGrainBatchId(),
                    "湿谷重量(kg)", r.getWetGrainWeightKg(),
                    "收割面积(亩)", r.getHarvestAreaMu(),
                    "水分(%)", r.getMoisturePercent(),
                    "杂质(%)", r.getImpurityPercent(),
                    "原粮等级", r.getGrainGrade(),
                    "仓温(℃)", r.getTemperature(),
                    "仓湿(%)", r.getHumidity()));
            node.setFileIds(StringUtils.hasText(r.getQualityReportFileId())
                    ? List.of(r.getQualityReportFileId()) : null);
            node.setChainStatus(r.getChainStatus());
            node.setTxId(r.getTxId());
            node.setDataHash(r.getReportHash());
            nodes.add(node);
        }

        // 碾米加工
        for (RiceMillingBatch m : ctx.millings) {
            TraceChainNodeVO node = newNode("MILLING_BATCH", m.getMillingBatchId(), "加工", "碾米加工", "MILLING_BATCH",
                    m.getMillingBatchId(), m.getProcessStartTime(), "加工厂 " + nz(m.getFactoryId()), "",
                    "出库 " + nz(m.getGrainOutWeightKg()) + "kg → 精米 " + nz(m.getRiceOutputWeightKg())
                            + "kg，产出率 " + (m.getYieldRate() == null ? "待核算" : m.getYieldRate() + "%"));
            node.setMetrics(metrics(
                    "原粮批次", m.getGrainBatchId(),
                    "稻谷出库(kg)", m.getGrainOutWeightKg(),
                    "精米产出(kg)", m.getRiceOutputWeightKg(),
                    "产出率(%)", m.getYieldRate(),
                    "抛光/色选参数", m.getProcessParams(),
                    "质检摘要", m.getQualitySummary()));
            node.setFileIds(StringUtils.hasText(m.getQualityReportFileId())
                    ? List.of(m.getQualityReportFileId()) : null);
            node.setChainStatus(m.getChainStatus());
            node.setTxId(m.getTxId());
            node.setDetailRoute("/rice/milling-batches");
            nodes.add(node);
        }

        // 成品批次
        for (RiceProductBatch p : ctx.products) {
            TraceChainNodeVO node = newNode("PRODUCT_BATCH", p.getProductBatchId(), "包装", "成品批次", "PRODUCT_BATCH",
                    p.getProductBatchId(), p.getCreateTime(), nz(p.getBrandName()),
                    "预期销售区域 " + nz(p.getExpectedSaleRegion()),
                    p.getProductName() + "（" + nz(p.getPackageSpec()) + "），执行标准 " + nz(p.getStandardNo()));
            node.setStatus(p.getStatus());
            node.setMetrics(metrics(
                    "产品名称", p.getProductName(),
                    "品牌", p.getBrandName(),
                    "品种", p.getRiceVariety(),
                    "包装规格", p.getPackageSpec(),
                    "执行标准", p.getStandardNo(),
                    "防伪码数量", codeCountOf(ctx, p.getProductBatchId()),
                    "认证证书", p.getCertificationFileIds() == null ? 0 : p.getCertificationFileIds().size()));
            node.setFileIds(p.getCertificationFileIds());
            node.setDetailRoute("/rice/product-batches");
            nodes.add(node);
        }

        // 一物一码（按成品批次聚合，避免一屏几百个码）
        for (RiceProductBatch p : ctx.products) {
            int count = codeCountOf(ctx, p.getProductBatchId());
            if (count == 0) {
                continue;
            }
            int scanned = (int) ctx.codeSamples.stream()
                    .filter(c -> p.getProductBatchId().equals(c.getProductBatchId()))
                    .filter(c -> c.getScanCount() != null && c.getScanCount() > 0)
                    .count();
            int scanCount = ctx.codeSamples.stream()
                    .filter(c -> p.getProductBatchId().equals(c.getProductBatchId()))
                    .mapToInt(c -> c.getScanCount() == null ? 0 : c.getScanCount()).sum();
            TraceChainNodeVO node = newNode("TRACE_CODE", p.getProductBatchId(), "流通", "一物一码", "TRACE_CODE",
                    p.getProductBatchId(), null, nz(p.getBrandName()), nz(p.getExpectedSaleRegion()),
                    "已赋码 " + count + " 枚，其中 " + scanned + " 枚已被扫码查询，累计扫码 " + scanCount + " 次");
            node.setMetrics(metrics(
                    "赋码数量", count,
                    "已扫码数量", scanned,
                    "累计扫码次数", scanCount,
                    "预期销售区域", p.getExpectedSaleRegion()));
            nodes.add(node);
        }
    }

    private int codeCountOf(ChainContext ctx, String productBatchId) {
        // 以数据库计数为准：codeSamples 只是抽样（最多 500 条），批量赋码时不能直接用它算数量
        Long count = traceCodeMapper.selectCount(new LambdaQueryWrapper<RiceTraceCode>()
                .eq(RiceTraceCode::getProductBatchId, productBatchId));
        return count == null ? 0 : count.intValue();
    }

    private TraceChainNodeVO newNode(String nodeType, String businessId, String stage, String title,
                                     String businessType, String id, LocalDateTime time, String org,
                                     String location, String summary) {
        TraceChainNodeVO node = new TraceChainNodeVO();
        node.setNodeKey(nodeType + ":" + id);
        node.setNodeType(nodeType);
        node.setStage(stage);
        node.setTitle(title);
        node.setBusinessType(businessType);
        node.setBusinessId(businessId);
        node.setTime(time == null ? "" : time.format(TIME_FMT));
        node.setOrganization(org == null ? "" : org);
        node.setLocation(location == null ? "" : location);
        node.setSummary(summary);
        node.setRisky(false);
        return node;
    }

    // ------------------------------------------------------------------
    // 2.2 关系边
    // ------------------------------------------------------------------
    private void buildEdges(ChainContext ctx, TraceChainVO vo) {
        List<TraceChainEdgeVO> edges = vo.getEdges();
        String fieldKey = ctx.field == null ? null : "FIELD:" + ctx.field.getFieldId();
        String plantingKey = ctx.planting == null ? null : "PLANTING_BATCH:" + ctx.planting.getPlantingBatchId();

        if (fieldKey != null && plantingKey != null) {
            edges.add(new TraceChainEdgeVO(fieldKey, plantingKey, "BOUND_TO", "地块绑定",
                    ctx.field.getAreaMu() == null ? "" : ctx.field.getAreaMu() + " 亩"));
        }
        for (RiceStorageReceipt r : ctx.receipts) {
            String receiptKey = "STORAGE_RECEIPT:" + r.getStorageReceiptId();
            if (plantingKey != null) {
                edges.add(new TraceChainEdgeVO(plantingKey, receiptKey, "HARVESTED_AS", "收割入库",
                        nz(r.getWetGrainWeightKg()) + " kg"));
            }
        }
        for (RiceMillingBatch m : ctx.millings) {
            String millingKey = "MILLING_BATCH:" + m.getMillingBatchId();
            for (RiceStorageReceipt r : ctx.receipts) {
                if (r.getGrainBatchId() != null && r.getGrainBatchId().equals(m.getGrainBatchId())) {
                    edges.add(new TraceChainEdgeVO("STORAGE_RECEIPT:" + r.getStorageReceiptId(), millingKey,
                            "MILLED_INTO", "出库加工", nz(m.getGrainOutWeightKg()) + " kg"));
                }
            }
        }
        for (RiceProductBatch p : ctx.products) {
            String productKey = "PRODUCT_BATCH:" + p.getProductBatchId();
            for (RiceMillingBatch m : ctx.millings) {
                if (p.getProductBatchId().equals(m.getProductBatchId())) {
                    edges.add(new TraceChainEdgeVO("MILLING_BATCH:" + m.getMillingBatchId(), productKey,
                            "PACKED_AS", "包装成品", nz(m.getRiceOutputWeightKg()) + " kg"));
                }
            }
            if (codeCountOf(ctx, p.getProductBatchId()) > 0) {
                edges.add(new TraceChainEdgeVO(productKey, "TRACE_CODE:" + p.getProductBatchId(),
                        "ACTIVATED_AS", "赋码激活", codeCountOf(ctx, p.getProductBatchId()) + " 枚"));
            }
        }
        // 去重（同一关系可能被多次命中）
        Set<String> seen = new LinkedHashSet<>();
        edges.removeIf(e -> !seen.add(e.getFrom() + "->" + e.getTo() + ":" + e.getRelation()));
    }

    // ------------------------------------------------------------------
    // 2.3 时间轴
    // ------------------------------------------------------------------
    private void buildTimeline(ChainContext ctx, TraceChainVO vo) {
        List<TraceTimelineItemVO> timeline = new ArrayList<>();

        if (ctx.planting != null && ctx.planting.getSowingDate() != null) {
            timeline.add(timelineItem(atStartOfDay(ctx.planting.getSowingDate()), "种植", "播种",
                    "品种 " + nz(ctx.planting.getRiceVariety()) + "，种子来源 " + nz(ctx.planting.getSeedSource()),
                    "PLANTING_BATCH", ctx.planting.getPlantingBatchId(), "",
                    ctx.planting.getChainStatus(), ctx.planting.getTxId(), null, false));
        }
        for (RiceFarmingLog log : ctx.farmingLogs) {
            String label = OPERATION_LABEL.getOrDefault(log.getOperationType() == null ? "" : log.getOperationType(),
                    log.getOperationType());
            String desc = nz(log.getMaterialName())
                    + (log.getMaterialDosage() == null ? "" : " 用量 " + log.getMaterialDosage()
                    + nz(log.getMaterialUnit()))
                    + (log.getSafeIntervalDays() == null ? "" : "，安全间隔期 " + log.getSafeIntervalDays() + " 天")
                    + (StringUtils.hasText(log.getDescription()) ? "；" + log.getDescription() : "");
            timeline.add(timelineItem(log.getOperationTime(), "种植", "农事·" + label, desc,
                    "FARMING_LOG", log.getLogId(), nz(log.getOperatorName()),
                    log.getChainStatus(), log.getTxId(), log.getProofFileIds(), false));
        }
        if (ctx.planting != null && ctx.planting.getActualHarvestDate() != null) {
            timeline.add(timelineItem(atStartOfDay(ctx.planting.getActualHarvestDate()), "种植", "收割",
                    "收割完成，进入收储环节", "PLANTING_BATCH", ctx.planting.getPlantingBatchId(), "",
                    ctx.planting.getChainStatus(), ctx.planting.getTxId(), null, false));
        }
        for (RiceStorageReceipt r : ctx.receipts) {
            timeline.add(timelineItem(r.getStorageTime(), "收储", "入库",
                    "入仓编号 " + nz(r.getWarehouseCode()) + "，湿谷 " + nz(r.getWetGrainWeightKg()) + "kg，等级 "
                            + nz(r.getGrainGrade()),
                    "STORAGE_RECEIPT", r.getStorageReceiptId(), "仓库 " + nz(r.getWarehouseId()),
                    r.getChainStatus(), r.getTxId(),
                    StringUtils.hasText(r.getQualityReportFileId()) ? List.of(r.getQualityReportFileId()) : null, false));
        }
        for (RiceQualityTest t : ctx.qualityTests) {
            boolean fail = "FAIL".equals(t.getOverallResult());
            timeline.add(timelineItem(t.getTestTime(), "质检", "质检·" + ("FAIL".equals(t.getOverallResult()) ? "不合格" : "合格"),
                    "检测机构 " + nz(t.getTestAgency()) + "，总体结论 " + nz(t.getOverallResult()),
                    "QUALITY_TEST", t.getQualityTestId(), nz(t.getTestAgency()),
                    t.getChainStatus(), t.getTxId(),
                    StringUtils.hasText(t.getReportFileId()) ? List.of(t.getReportFileId()) : null, fail));
        }
        for (RiceMillingBatch m : ctx.millings) {
            timeline.add(timelineItem(m.getProcessEndTime() == null ? m.getProcessStartTime() : m.getProcessEndTime(),
                    "加工", "碾米加工",
                    "出库 " + nz(m.getGrainOutWeightKg()) + "kg，精米 " + nz(m.getRiceOutputWeightKg())
                            + "kg，产出率 " + nz(m.getYieldRate()) + "%",
                    "MILLING_BATCH", m.getMillingBatchId(), "加工厂 " + nz(m.getFactoryId()),
                    m.getChainStatus(), m.getTxId(),
                    StringUtils.hasText(m.getQualityReportFileId()) ? List.of(m.getQualityReportFileId()) : null,
                    m.getYieldRate() != null && m.getYieldRate().compareTo(new BigDecimal("75")) > 0));
        }
        for (RiceProductBatch p : ctx.products) {
            timeline.add(timelineItem(p.getCreateTime(), "包装", "成品包装",
                    p.getProductName() + "（" + nz(p.getPackageSpec()) + "），执行标准 " + nz(p.getStandardNo()),
                    "PRODUCT_BATCH", p.getProductBatchId(), nz(p.getBrandName()),
                    null, null, p.getCertificationFileIds(), false));
        }
        // 扫码流通：按区域汇总最近扫码事件
        List<RiceScanLog> scans = recentScans(ctx);
        for (RiceScanLog s : scans) {
            timeline.add(timelineItem(s.getScanTime(), "流通", "消费者扫码",
                    "区域 " + nz(s.getRegion()) + (Boolean.TRUE.equals(s.getFirstScan()) ? "（首次查询）" : "（重复查询）"),
                    "TRACE_CODE", s.getTraceCode(), "", null, null, null,
                    "HIGH".equals(s.getRiskLevel())));
        }

        timeline.sort(Comparator.comparing((TraceTimelineItemVO i) -> i.getTime() == null ? "" : i.getTime()));
        vo.setTimeline(timeline);

        // 召回冻结提示
        List<String> tips = vo.getTips();
        if (!ctx.products.isEmpty()) {
            List<String> productIds = ctx.products.stream().map(RiceProductBatch::getProductBatchId).toList();
            List<RiceRecallItem> recallItems = recallItemMapper.selectList(new LambdaQueryWrapper<RiceRecallItem>()
                    .in(RiceRecallItem::getBusinessId, productIds)
                    .in(RiceRecallItem::getHandleStatus, RECALL_ACTIVE_STATUS));
            if (!recallItems.isEmpty()) {
                tips.add("该链路产品正处于召回处置中（" + recallItems.size() + " 条明细），出入库与销售已被冻结");
            }
        }
        if (ctx.qualityTests.stream().anyMatch(t -> "FAIL".equals(t.getOverallResult()))) {
            tips.add("链路中存在不合格质检记录，后续加工/赋码环节会被防错规则拦截");
        }
        if (ctx.planting != null && ctx.receipts.isEmpty()) {
            tips.add("该种植批次尚未收储入库，链路止于种植环节");
        }
        if (!ctx.receipts.isEmpty() && ctx.millings.isEmpty()) {
            tips.add("尚未创建碾米加工批次，链路止于收储环节");
        }
        if (!ctx.millings.isEmpty() && ctx.products.isEmpty()) {
            tips.add("加工批次尚未关联成品批次，链路止于加工环节");
        }
        if (ctx.products.isEmpty() && ctx.planting == null) {
            tips.add("未找到上游种植信息，链路不完整，请检查批次绑定关系");
        }
    }

    private List<RiceScanLog> recentScans(ChainContext ctx) {
        List<String> codes = ctx.codeSamples.stream().map(RiceTraceCode::getTraceCode)
                .filter(StringUtils::hasText).limit(100).toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        return scanLogMapper.selectList(new LambdaQueryWrapper<RiceScanLog>()
                .in(RiceScanLog::getTraceCode, codes)
                .orderByDesc(RiceScanLog::getScanTime)
                .last("LIMIT 20"));
    }

    private TraceTimelineItemVO timelineItem(LocalDateTime time, String stage, String title, String description,
                                             String businessType, String businessId, String org,
                                             String chainStatus, String txId, List<String> fileIds, boolean risky) {
        TraceTimelineItemVO item = new TraceTimelineItemVO();
        item.setTime(time == null ? "" : time.format(TIME_FMT));
        item.setStage(stage);
        item.setTitle(title);
        item.setDescription(description);
        item.setBusinessType(businessType);
        item.setBusinessId(businessId);
        item.setOrganization(org);
        item.setChainStatus(chainStatus);
        item.setTxId(txId);
        item.setFileIds(fileIds);
        item.setRisky(risky);
        return item;
    }

    // ------------------------------------------------------------------
    // 2.4 汇总与存证
    // ------------------------------------------------------------------
    private void fillSummary(ChainContext ctx, TraceChainVO vo) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nodeCount", vo.getNodes().size());
        summary.put("farmingLogCount", ctx.farmingLogs.size());
        summary.put("qualityTestCount", ctx.qualityTests.size());
        summary.put("storageCount", ctx.receipts.size());
        summary.put("millingCount", ctx.millings.size());
        summary.put("productBatchCount", ctx.products.size());
        summary.put("traceCodeCount", ctx.codeCount);
        summary.put("scanCount", ctx.scanCount);
        summary.put("scannedCodeCount", ctx.scannedCodeCount);
        summary.put("dryGrainWeightKg", ctx.receipts.stream()
                .map(RiceStorageReceipt::getWetGrainWeightKg)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("riceOutputWeightKg", ctx.millings.stream()
                .map(RiceMillingBatch::getRiceOutputWeightKg)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setSummary(summary);
    }

    private void fillChainProofs(TraceChainVO vo) {
        List<Map<String, Object>> proofs = new ArrayList<>();
        int success = 0;
        int total = 0;
        for (TraceChainNodeVO node : vo.getNodes()) {
            if (!StringUtils.hasText(node.getBusinessType()) || "TRACE_CODE".equals(node.getBusinessType())) {
                continue;
            }
            RiceChainProof proof = chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                    .eq(RiceChainProof::getBusinessType, node.getBusinessType())
                    .eq(RiceChainProof::getBusinessId, node.getBusinessId())
                    .last("LIMIT 1"));
            if (proof == null) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("stage", node.getStage());
            map.put("businessType", proof.getBusinessType());
            map.put("businessId", proof.getBusinessId());
            map.put("dataHash", proof.getDataHash());
            map.put("txId", proof.getTxId());
            map.put("blockHeight", proof.getBlockHeight());
            map.put("chainTime", proof.getChainTime() == null ? "" : proof.getChainTime().format(TIME_FMT));
            map.put("chainStatus", proof.getChainStatus());
            map.put("fileHashes", proof.getFileHashes());
            proofs.add(map);
            total++;
            if ("SUCCESS".equals(proof.getChainStatus())) {
                success++;
            }
            // 同步节点上的链上信息
            node.setChainStatus(proof.getChainStatus());
            node.setTxId(proof.getTxId());
        }
        vo.setChainProofs(proofs);
        if (vo.getSummary() != null) {
            vo.getSummary().put("chainProofCount", total);
            vo.getSummary().put("chainSuccessRate", total == 0 ? 0 : success * 100 / total);
        }
        if (total > 0 && success < total) {
            vo.getTips().add("链上存证 " + total + " 条，其中 " + (total - success) + " 条尚未成功上链");
        }
    }

    // ==================================================================
    // 3. 影响范围
    // ==================================================================
    @Override
    public TraceImpactVO impact(String businessType, String businessId) {
        ChainContext ctx = loadContext(businessType, businessId);
        TraceImpactVO vo = new TraceImpactVO();
        vo.setRootType(businessType);
        vo.setRootId(businessId);
        vo.setRootLabel(rootLabel(ctx, businessType, businessId));
        vo.setTips(new ArrayList<>());

        vo.setGrainBatches(ctx.receipts.stream().map(RiceStorageReceipt::getGrainBatchId)
                .filter(StringUtils::hasText).distinct().toList());
        vo.setMillingBatches(ctx.millings.stream().map(RiceMillingBatch::getMillingBatchId).toList());

        List<Map<String, Object>> products = new ArrayList<>();
        for (RiceProductBatch p : ctx.products) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productBatchId", p.getProductBatchId());
            map.put("productName", p.getProductName());
            map.put("brandName", p.getBrandName());
            map.put("packageSpec", p.getPackageSpec());
            map.put("status", p.getStatus());
            map.put("expectedSaleRegion", p.getExpectedSaleRegion());
            map.put("traceCodeCount", codeCountOf(ctx, p.getProductBatchId()));
            products.add(map);
        }
        vo.setProductBatches(products);

        List<String> codes = ctx.codeSamples.stream().map(RiceTraceCode::getTraceCode).toList();
        vo.setTraceCodeCount(ctx.codeCount);
        vo.setTraceCodeSamples(codes.stream().limit(50).toList());
        vo.setQuantity(ctx.codeCount);
        vo.setScannedCodeCount(ctx.scannedCodeCount);
        vo.setFrozenCodeCount(ctx.frozenCodeCount);

        BigDecimal weight = ctx.millings.stream().map(RiceMillingBatch::getRiceOutputWeightKg)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weight.compareTo(BigDecimal.ZERO) == 0) {
            // 加工未完成时退化为按湿谷重量估算
            weight = ctx.receipts.stream().map(RiceStorageReceipt::getWetGrainWeightKg)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        vo.setWeightKg(weight);

        // 扫码区域分布（有扫码日志用真实分布，否则退回预期销售区域）
        List<Map<String, Object>> regions = new ArrayList<>();
        if (!codes.isEmpty()) {
            List<RiceScanLog> scans = scanLogMapper.selectList(new LambdaQueryWrapper<RiceScanLog>()
                    .in(RiceScanLog::getTraceCode, codes.stream().limit(200).toList()));
            Map<String, Integer> counter = new LinkedHashMap<>();
            for (RiceScanLog s : scans) {
                String region = StringUtils.hasText(s.getRegion()) ? s.getRegion() : "未知区域";
                counter.merge(region, 1, Integer::sum);
            }
            counter.forEach((k, v) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("region", k);
                m.put("count", v);
                m.put("source", "SCAN");
                regions.add(m);
            });
        }
        if (regions.isEmpty()) {
            for (RiceProductBatch p : ctx.products) {
                if (!StringUtils.hasText(p.getExpectedSaleRegion())) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("region", p.getExpectedSaleRegion());
                m.put("count", codeCountOf(ctx, p.getProductBatchId()));
                m.put("source", "EXPECTED");
                regions.add(m);
            }
        }
        vo.setRegions(regions);

        if (ctx.codeCount > 0) {
            vo.getTips().add("影响范围内共 " + ctx.codeCount + " 枚防伪码，其中 " + ctx.scannedCodeCount
                    + " 枚已被消费者扫码（风险最高，需优先通知下架）");
        }
        if (ctx.products.isEmpty()) {
            vo.getTips().add("该批次尚未形成成品批次，影响范围仅限原粮/在制品");
        }
        if (ctx.millings.stream().anyMatch(m -> m.getRiceOutputWeightKg() == null)) {
            vo.getTips().add("存在未完成加工的批次，重量按湿谷重量估算");
        }
        return vo;
    }

    // ==================================================================
    // 工具方法
    // ==================================================================
    private Map<String, Object> metrics(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object value = kv[i + 1];
            if (value == null || "".equals(value)) {
                continue;
            }
            map.put(String.valueOf(kv[i]), value);
        }
        return map;
    }

    private String joinRegion(RiceField f) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(f.getProvince())) {
            sb.append(f.getProvince());
        }
        if (StringUtils.hasText(f.getCity())) {
            sb.append(f.getCity());
        }
        if (StringUtils.hasText(f.getDistrict())) {
            sb.append(f.getDistrict());
        }
        if (StringUtils.hasText(f.getAddress())) {
            sb.append(f.getAddress());
        }
        return sb.toString();
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private String nz(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    /** 供召回复用：取链路内的成品批次 ID */
    public List<String> productBatchIdsOf(String businessType, String businessId) {
        return loadContext(businessType, businessId).products.stream()
                .map(RiceProductBatch::getProductBatchId).toList();
    }

    /** 供防错复用：判断成品批次是否处于召回冻结 */
    public boolean isProductUnderRecall(String productBatchId) {
        if (!StringUtils.hasText(productBatchId)) {
            return false;
        }
        Long count = recallItemMapper.selectCount(new LambdaQueryWrapper<RiceRecallItem>()
                .eq(RiceRecallItem::getBusinessId, productBatchId)
                .in(RiceRecallItem::getHandleStatus, RECALL_ACTIVE_STATUS));
        return count != null && count > 0;
    }

    /** 供防错复用：判断防伪码是否处于召回冻结 */
    public boolean isTraceCodeUnderRecall(String traceCode) {
        if (!StringUtils.hasText(traceCode)) {
            return false;
        }
        Long count = recallItemMapper.selectCount(new LambdaQueryWrapper<RiceRecallItem>()
                .eq(RiceRecallItem::getBusinessId, traceCode)
                .in(RiceRecallItem::getHandleStatus, RECALL_ACTIVE_STATUS));
        return count != null && count > 0;
    }
}
