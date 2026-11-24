package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.qukuailian.entity.RiceFarmingLog;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RiceQualityTest;
import com.itheima.qukuailian.entity.RiceRecallItem;
import com.itheima.qukuailian.entity.RiceRecallTask;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceFarmingLogMapper;
import com.itheima.qukuailian.mapper.RiceMillingBatchMapper;
import com.itheima.qukuailian.mapper.RiceQualityTestMapper;
import com.itheima.qukuailian.mapper.RiceRecallItemMapper;
import com.itheima.qukuailian.mapper.RiceRecallTaskMapper;
import com.itheima.qukuailian.mapper.RiceRiskWarningMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.RiceAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 质量分析实现：
 * <ol>
 *   <li>质量趋势：近 6 个月质检合格率</li>
 *   <li>异常分布：风险预警按类型聚合（含质量、窜货、重复扫码、召回）</li>
 *   <li>召回绩效：任务数、关闭率、平均回收率、平均处置周期</li>
 *   <li>风险评分：五条可解释规则加权，输出等级 + 主要影响因素 + 建议</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiceAnalyticsServiceImpl implements RiceAnalyticsService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 精米产出率合理区间（与加工服务保持一致） */
    private static final BigDecimal YIELD_MIN = new BigDecimal("55");
    private static final BigDecimal YIELD_MAX = new BigDecimal("75");

    private final RiceQualityTestMapper qualityTestMapper;
    private final RiceRiskWarningMapper riskWarningMapper;
    private final RiceRecallTaskMapper recallTaskMapper;
    private final RiceRecallItemMapper recallItemMapper;
    private final RiceFarmingLogMapper farmingLogMapper;
    private final RiceMillingBatchMapper millingBatchMapper;
    private final RiceTraceCodeMapper traceCodeMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qualityTrend", qualityTrend());
        result.put("warningDistribution", warningDistribution());
        result.put("recallPerformance", recallPerformance());
        result.put("riskProfile", riskProfile());
        return result;
    }

    /** FX-01：近 6 个月质检合格率 */
    private List<Map<String, Object>> qualityTrend() {
        LocalDateTime from = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).toLocalDate().atStartOfDay();
        List<RiceQualityTest> tests = qualityTestMapper.selectList(new LambdaQueryWrapper<RiceQualityTest>()
                .ge(RiceQualityTest::getTestTime, from)
                .orderByAsc(RiceQualityTest::getTestTime));

        Map<String, int[]> bucket = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            bucket.put(YearMonth.now().minusMonths(i).format(MONTH_FMT), new int[]{0, 0});
        }
        for (RiceQualityTest test : tests) {
            if (test.getTestTime() == null) {
                continue;
            }
            String month = test.getTestTime().format(MONTH_FMT);
            int[] counter = bucket.get(month);
            if (counter == null) {
                continue;
            }
            if ("PASS".equals(test.getOverallResult())) {
                counter[0]++;
            } else if ("FAIL".equals(test.getOverallResult())) {
                counter[1]++;
            }
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        bucket.forEach((month, counter) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            int pass = counter[0];
            int fail = counter[1];
            int total = pass + fail;
            item.put("month", month);
            item.put("passCount", pass);
            item.put("failCount", fail);
            item.put("passRate", total == 0 ? 0 : pass * 100 / total);
            trend.add(item);
        });
        return trend;
    }

    /** FX-01/FX-02：异常类型分布 */
    private List<Map<String, Object>> warningDistribution() {
        List<RiceRiskWarning> warnings = riskWarningMapper.selectList(new LambdaQueryWrapper<RiceRiskWarning>()
                .orderByDesc(RiceRiskWarning::getCreateTime));
        Map<String, Integer> counter = new LinkedHashMap<>();
        Map<String, Integer> unhandled = new LinkedHashMap<>();
        for (RiceRiskWarning warning : warnings) {
            String type = warning.getWarningType() == null ? "OTHER" : warning.getWarningType();
            counter.merge(type, 1, Integer::sum);
            if (!Boolean.TRUE.equals(warning.getHandled())) {
                unhandled.merge(type, 1, Integer::sum);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        counter.forEach((type, count) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", type);
            item.put("label", WARNING_LABEL.getOrDefault(type, type));
            item.put("count", count);
            item.put("unhandled", unhandled.getOrDefault(type, 0));
            list.add(item);
        });
        list.sort((a, b) -> (Integer) b.get("count") - (Integer) a.get("count"));
        return list;
    }

    private static final Map<String, String> WARNING_LABEL = Map.of(
            "YIELD_BALANCE", "产量/产出率异常",
            "CHANNEL_CONFLICT", "窜货风险",
            "REPEAT_SCAN", "重复扫码",
            "UNACTIVATED_SCAN", "未激活扫码",
            "CHAIN_VERIFY_FAILED", "链上核验失败",
            "RECALL_TRIGGERED", "召回事件");

    /** JK-06：召回绩效 */
    private Map<String, Object> recallPerformance() {
        List<RiceRecallTask> tasks = recallTaskMapper.selectList(new LambdaQueryWrapper<RiceRecallTask>()
                .orderByDesc(RiceRecallTask::getCreateTime));
        long closed = tasks.stream().filter(t -> "CLOSED".equals(t.getStatus())).count();
        long released = tasks.stream()
                .filter(t -> List.of("RELEASED", "HANDLING", "CLOSED").contains(t.getStatus())).count();

        // 平均处置周期：发布 → 关闭
        double avgDays = tasks.stream()
                .filter(t -> t.getReleasedAt() != null && t.getClosedAt() != null)
                .mapToLong(t -> ChronoUnit.HOURS.between(t.getReleasedAt(), t.getClosedAt()))
                .average().orElse(0);
        double avgHours = Math.round(avgDays * 10) / 10.0;

        // 平均回收率
        List<RiceRecallItem> items = recallItemMapper.selectList(new LambdaQueryWrapper<RiceRecallItem>());
        long settled = items.stream()
                .filter(i -> List.of("RECOVERED", "ISOLATED", "RETURNED", "SCRAPPED").contains(i.getHandleStatus()))
                .count();
        int recoveryRate = items.isEmpty() ? 0 : (int) (settled * 100 / items.size());

        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("taskCount", tasks.size());
        performance.put("releasedCount", released);
        performance.put("closedCount", closed);
        performance.put("closeRate", tasks.isEmpty() ? 0 : (int) (closed * 100 / tasks.size()));
        performance.put("avgHandleHours", avgHours);
        performance.put("recoveryRate", recoveryRate);
        performance.put("itemCount", items.size());
        performance.put("unRecoveredCount", (int) items.stream()
                .filter(i -> List.of("PENDING", "UNRECOVERED").contains(i.getHandleStatus())).count());
        return performance;
    }

    /** FX-03/FX-04：可解释风险评分 */
    private Map<String, Object> riskProfile() {
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<String> factors = new ArrayList<>();
        int score = 0;

        // 1) 未关闭召回（权重最高）
        Long openRecalls = recallTaskMapper.selectCount(new LambdaQueryWrapper<RiceRecallTask>()
                .in(RiceRecallTask::getStatus, List.of("RELEASED", "HANDLING")));
        long openRecallCount = openRecalls == null ? 0 : openRecalls;
        if (openRecallCount > 0) {
            score += (int) Math.min(40, openRecallCount * 20);
            factors.add("存在 " + openRecallCount + " 个未关闭的召回任务（+"
                    + Math.min(40, openRecallCount * 20) + " 分）");
        }

        // 2) 近 30 天不合格质检
        Long failTests = qualityTestMapper.selectCount(new LambdaQueryWrapper<RiceQualityTest>()
                .eq(RiceQualityTest::getOverallResult, "FAIL")
                .ge(RiceQualityTest::getTestTime, monthAgo));
        long failCount = failTests == null ? 0 : failTests;
        if (failCount > 0) {
            score += (int) Math.min(20, failCount * 5);
            factors.add("近 30 天不合格质检 " + failCount + " 次（+" + Math.min(20, failCount * 5) + " 分）");
        }

        // 3) 休药期未满仍在库的批次
        List<RiceFarmingLog> pesticideLogs = farmingLogMapper.selectList(new LambdaQueryWrapper<RiceFarmingLog>()
                .eq(RiceFarmingLog::getOperationType, "PESTICIDE")
                .isNotNull(RiceFarmingLog::getSafeIntervalDays)
                .ge(RiceFarmingLog::getOperationTime, LocalDateTime.now().minusDays(60)));
        long inSafePeriod = pesticideLogs.stream().filter(log -> log.getOperationTime() != null
                        && log.getOperationTime().plusDays(log.getSafeIntervalDays()).isAfter(LocalDateTime.now()))
                .map(RiceFarmingLog::getPlantingBatchId).distinct().count();
        if (inSafePeriod > 0) {
            score += (int) Math.min(15, inSafePeriod * 5);
            factors.add("有 " + inSafePeriod + " 个种植批次仍处于休药期（+" + Math.min(15, inSafePeriod * 5) + " 分）");
        }

        // 4) 产出率超限的加工批次
        List<RiceMillingBatch> millings = millingBatchMapper.selectList(new LambdaQueryWrapper<RiceMillingBatch>()
                .isNotNull(RiceMillingBatch::getYieldRate));
        long abnormalYield = millings.stream()
                .filter(m -> m.getYieldRate().compareTo(YIELD_MIN) < 0 || m.getYieldRate().compareTo(YIELD_MAX) > 0)
                .count();
        if (abnormalYield > 0) {
            score += (int) Math.min(15, abnormalYield * 3);
            factors.add("精米产出率超出 " + YIELD_MIN + "%-" + YIELD_MAX + "% 的加工批次 " + abnormalYield
                    + " 个（+" + Math.min(15, abnormalYield * 3) + " 分）");
        }

        // 5) 高频重复扫码（疑似复制/窜货）
        Long repeated = traceCodeMapper.selectCount(new LambdaQueryWrapper<RiceTraceCode>()
                .ge(RiceTraceCode::getScanCount, 5));
        long repeatedCount = repeated == null ? 0 : repeated;
        if (repeatedCount > 0) {
            score += (int) Math.min(10, repeatedCount);
            factors.add("扫码次数≥5 的防伪码 " + repeatedCount + " 枚，存在复制/窜货嫌疑（+"
                    + Math.min(10, repeatedCount) + " 分）");
        }

        score = Math.min(100, score);
        String level = score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";
        List<String> suggestions = new ArrayList<>();
        if (openRecallCount > 0) {
            suggestions.add("优先推进未关闭召回任务的终端下架与回收登记，回收率是监管关注的核心指标");
        }
        if (failCount > 0) {
            suggestions.add("对不合格批次创建不合格品处置单，评审通过后系统会自动隔离，避免继续流转");
        }
        if (inSafePeriod > 0) {
            suggestions.add("休药期未满的批次禁止收割入库，建议在农事记录中补全安全间隔期以便自动拦截");
        }
        if (abnormalYield > 0) {
            suggestions.add("核查产出率异常批次的工艺参数（抛光/色选）与实际投料量，排除计量或掺杂问题");
        }
        if (repeatedCount > 0) {
            suggestions.add("对高频扫码码值做窜货预警比对，必要时冻结可疑码");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前无显著风险信号，保持常规巡检频率即可");
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("score", score);
        profile.put("level", level);
        profile.put("factors", factors);
        profile.put("suggestions", suggestions);
        profile.put("calculatedAt", LocalDateTime.now().withNano(0).toString());
        profile.put("method", "规则加权：未关闭召回40 + 不合格质检20 + 休药期15 + 产出率异常15 + 重复扫码10");
        return profile;
    }
}
