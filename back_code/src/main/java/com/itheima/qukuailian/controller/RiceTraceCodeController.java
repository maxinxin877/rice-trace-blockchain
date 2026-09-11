package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageQuery;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.TraceCodeActivateDTO;
import com.itheima.qukuailian.dto.TraceCodeGenerateDTO;
import com.itheima.qukuailian.entity.RiceGenerateTask;
import com.itheima.qukuailian.entity.RiceScanLog;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.service.RiceProductBatchService;
import com.itheima.qukuailian.service.RiceTraceCodeService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 品牌防伪接口（接口文档 v1.1 §5.24-§5.27）
 * <p>分页参数同时兼容 pageNo（文档规范）与 page（前端常用写法）。</p>
 */
@RestController
@RequestMapping("/rice/trace-codes")
@RequiredArgsConstructor
public class RiceTraceCodeController {

    private final RiceTraceCodeService traceCodeService;
    private final RiceProductBatchService productBatchService;

    /** 批量生成防伪码 */
    @PostMapping("/generate")
    @RequirePermission(PermissionConstants.RICE_TRACE_CODE_GENERATE)
    public Result<RiceGenerateTask> generate(@Valid @RequestBody TraceCodeGenerateDTO dto,
                                             HttpServletRequest request) {
        return Result.success(traceCodeService.generate(dto, request.getRemoteAddr()));
    }

    /** 激活防伪码 */
    @PostMapping("/activate")
    @RequirePermission(PermissionConstants.RICE_TRACE_CODE_ACTIVATE)
    public Result<Map<String, Object>> activate(@Valid @RequestBody TraceCodeActivateDTO dto,
                                                HttpServletRequest request) {
        return Result.success(traceCodeService.activate(dto, request.getRemoteAddr()));
    }

    /** 查询防伪码 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_TRACE_CODE_VIEW)
    public Result<PageResult<RiceTraceCode>> page(PageQuery pageQuery,
                                                  @RequestParam(required = false) String productBatchId,
                                                  @RequestParam(required = false) String traceCode,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String riskLevel) {
        IPage<RiceTraceCode> result = traceCodeService.page(pageQuery.resolvePageNo(), pageQuery.resolvePageSize(),
                productBatchId, traceCode, status, riskLevel);
        return Result.success(PageResult.of(result));
    }

    /** 防伪码扫码记录（防伪码列表"扫码记录"弹窗） */
    @GetMapping("/{traceCode}/scan-logs")
    @RequirePermission(PermissionConstants.RICE_TRACE_CODE_VIEW)
    public Result<PageResult<RiceScanLog>> scanLogs(@PathVariable String traceCode, PageQuery pageQuery) {
        IPage<RiceScanLog> result = productBatchService
                .pageScanLogs(traceCode, pageQuery.resolvePageNo(), pageQuery.resolvePageSize());
        return Result.success(PageResult.of(result));
    }
}
