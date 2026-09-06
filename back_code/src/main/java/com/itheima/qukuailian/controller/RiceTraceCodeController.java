package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.TraceCodeActivateDTO;
import com.itheima.qukuailian.dto.TraceCodeGenerateDTO;
import com.itheima.qukuailian.entity.RiceGenerateTask;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.service.RiceTraceCodeService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 品牌防伪接口（接口文档 v1.1 §5.24-§5.27）
 */
@RestController
@RequestMapping("/rice/trace-codes")
@RequiredArgsConstructor
public class RiceTraceCodeController {

    private final RiceTraceCodeService traceCodeService;

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
    public Result<PageResult<RiceTraceCode>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                  @RequestParam(defaultValue = "20") long pageSize,
                                                  @RequestParam(required = false) String productBatchId,
                                                  @RequestParam(required = false) String traceCode,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String riskLevel) {
        IPage<RiceTraceCode> result = traceCodeService
                .page(pageNo, pageSize, productBatchId, traceCode, status, riskLevel);
        return Result.success(PageResult.of(result));
    }
}
