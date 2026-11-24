package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.service.RiceAnalyticsService;
import com.itheima.qukuailian.utils.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 质量分析接口（需求文档 §九 FX-01~FX-04、§8.6 数据分析辅助决策）
 */
@RestController
@RequestMapping("/rice/analytics")
@RequiredArgsConstructor
public class RiceAnalyticsController {

    private final RiceAnalyticsService analyticsService;

    /** 综合分析：质量趋势 + 异常分布 + 召回绩效 + 风险评分 */
    @GetMapping("/overview")
    @RequirePermission(PermissionConstants.RICE_DASHBOARD_VIEW)
    public Result<Map<String, Object>> overview() {
        return Result.success(analyticsService.overview());
    }
}
