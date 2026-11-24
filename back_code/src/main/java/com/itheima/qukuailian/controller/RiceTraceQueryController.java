package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.service.RiceTraceQueryService;
import com.itheima.qukuailian.utils.PermissionConstants;
import com.itheima.qukuailian.vo.TraceChainVO;
import com.itheima.qukuailian.vo.TraceImpactVO;
import com.itheima.qukuailian.vo.TraceResolveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 追溯查询接口（需求文档 §六-3、§九 ZS-01/ZS-02）
 * <p>与消费者端的 {@code /rice/trace/mini/**} 区分：本组接口面向管理端与监管端，
 * 返回完整链路（含未公开的供应商、成本、操作人等信息）。</p>
 */
@RestController
@RequestMapping("/rice/trace")
@RequiredArgsConstructor
public class RiceTraceQueryController {

    private final RiceTraceQueryService traceQueryService;

    /** 识别关键词类型（单品码/成品批次/加工批次/原粮批次/入库单/种植批次/地块） */
    @GetMapping("/resolve")
    @RequirePermission(PermissionConstants.RICE_TRACE_QUERY_VIEW)
    public Result<TraceResolveVO> resolve(@RequestParam String keyword) {
        return Result.success(traceQueryService.resolve(keyword));
    }

    /** 一站式检索：识别 + 出链路，管理端搜索框一次请求搞定 */
    @GetMapping("/search")
    @RequirePermission(PermissionConstants.RICE_TRACE_QUERY_VIEW)
    public Result<Map<String, Object>> search(@RequestParam String keyword) {
        TraceResolveVO resolved = traceQueryService.resolve(keyword);
        TraceChainVO chain = traceQueryService.chain(resolved.getMatchedType(), resolved.getMatchedId(), "BOTH");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resolved", resolved);
        data.put("chain", chain);
        return Result.success(data);
    }

    /** 追溯链路（时间轴 + 上下游关系 + 链上存证） */
    @GetMapping("/chain")
    @RequirePermission(PermissionConstants.RICE_TRACE_QUERY_VIEW)
    public Result<TraceChainVO> chain(@RequestParam String businessType,
                                      @RequestParam String businessId,
                                      @RequestParam(defaultValue = "BOTH") String direction) {
        return Result.success(traceQueryService.chain(businessType, businessId, direction));
    }

    /** 影响范围分析（召回前评估） */
    @GetMapping("/impact")
    @RequirePermission(PermissionConstants.RICE_TRACE_QUERY_VIEW)
    public Result<TraceImpactVO> impact(@RequestParam String businessType,
                                        @RequestParam String businessId) {
        return Result.success(traceQueryService.impact(businessType, businessId));
    }
}
