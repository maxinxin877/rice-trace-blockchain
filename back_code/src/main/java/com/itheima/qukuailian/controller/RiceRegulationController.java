package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.RiskWarningHandleDTO;
import com.itheima.qukuailian.dto.YieldBalanceCheckDTO;
import com.itheima.qukuailian.entity.RiceAuditLog;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceYieldBalance;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.RiceRegulationService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 监管与审计接口（接口文档 v1.1 §7）+ 看板统计（§5.28）
 */
@RestController
@RequestMapping("/rice/regulation")
@RequiredArgsConstructor
public class RiceRegulationController {

    private final RiceRegulationService regulationService;
    private final AuditLogService auditLogService;

    /** 执行产量平衡校验 */
    @PostMapping("/yield-balance/check")
    @RequirePermission(PermissionConstants.RICE_REGULATION_CHECK)
    public Result<RiceYieldBalance> checkYieldBalance(@Valid @RequestBody YieldBalanceCheckDTO dto,
                                                      HttpServletRequest request) {
        return Result.success(regulationService.checkYieldBalance(dto, request.getRemoteAddr()));
    }

    /** 查询产量平衡校验结果 */
    @GetMapping("/yield-balance/results")
    @RequirePermission(PermissionConstants.RICE_REGULATION_VIEW)
    public Result<PageResult<RiceYieldBalance>> yieldBalanceResults(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String checkId,
            @RequestParam(required = false) String plantingBatchId,
            @RequestParam(required = false) String grainBatchId,
            @RequestParam(required = false) String productBatchId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        IPage<RiceYieldBalance> page = regulationService.pageYieldBalanceResults(
                pageNo, pageSize, checkId, plantingBatchId, grainBatchId, productBatchId, result, startTime, endTime);
        return Result.success(PageResult.of(page));
    }

    /** 查询风险预警 */
    @GetMapping("/risk-warnings")
    @RequirePermission(PermissionConstants.RICE_REGULATION_VIEW)
    public Result<PageResult<RiceRiskWarning>> riskWarnings(@RequestParam(defaultValue = "1") long pageNo,
                                                            @RequestParam(defaultValue = "20") long pageSize,
                                                            @RequestParam(required = false) String warningType,
                                                            @RequestParam(required = false) String riskLevel,
                                                            @RequestParam(required = false) String businessId,
                                                            @RequestParam(required = false) Boolean handled) {
        IPage<RiceRiskWarning> page = regulationService.pageRiskWarnings(
                pageNo, pageSize, warningType, riskLevel, businessId, handled);
        return Result.success(PageResult.of(page));
    }

    /** 处理风险预警 */
    @PutMapping("/risk-warnings/{warningId}/handle")
    @RequirePermission(PermissionConstants.RICE_REGULATION_CHECK)
    public Result<RiceRiskWarning> handleWarning(@PathVariable String warningId,
                                                 @Valid @RequestBody RiskWarningHandleDTO dto,
                                                 HttpServletRequest request) {
        return Result.success(regulationService.handleWarning(warningId, dto, request.getRemoteAddr()));
    }

    /** 查询审计日志 */
    @GetMapping("/audit-logs")
    @RequirePermission(PermissionConstants.RICE_AUDIT_VIEW)
    public Result<PageResult<RiceAuditLog>> auditLogs(@RequestParam(defaultValue = "1") long pageNo,
                                                      @RequestParam(defaultValue = "20") long pageSize,
                                                      @RequestParam(required = false) String businessType,
                                                      @RequestParam(required = false) String businessId,
                                                      @RequestParam(required = false) String operatorId,
                                                      @RequestParam(required = false) String operationType,
                                                      @RequestParam(required = false) String startTime,
                                                      @RequestParam(required = false) String endTime) {
        IPage<RiceAuditLog> page = auditLogService.page(
                pageNo, pageSize, businessType, businessId, operatorId, operationType, startTime, endTime);
        return Result.success(PageResult.of(page));
    }

    /** 水稻模块总览统计 */
    @GetMapping("/dashboard/summary")
    @RequirePermission(PermissionConstants.RICE_DASHBOARD_VIEW)
    public Result<Map<String, Object>> dashboardSummary() {
        return Result.success(regulationService.dashboardSummary());
    }
}
