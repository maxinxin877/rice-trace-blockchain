package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.MillingBatchCompleteDTO;
import com.itheima.qukuailian.dto.MillingBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.service.RiceMillingBatchService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 碾米加工接口（接口文档 v1.1 §5.18-§5.21）
 */
@RestController
@RequestMapping("/rice/milling-batches")
@RequiredArgsConstructor
public class RiceMillingBatchController {

    private final RiceMillingBatchService millingBatchService;

    /** 创建加工批次 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_MILLING_CREATE)
    public Result<RiceMillingBatch> create(@Valid @RequestBody MillingBatchCreateDTO dto,
                                           HttpServletRequest request) {
        return Result.success(millingBatchService.create(dto, request.getRemoteAddr()));
    }

    /** 分页查询加工批次 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_MILLING_VIEW)
    public Result<PageResult<RiceMillingBatch>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                     @RequestParam(defaultValue = "20") long pageSize,
                                                     @RequestParam(required = false) String grainBatchId,
                                                     @RequestParam(required = false) String productBatchId,
                                                     @RequestParam(required = false) String factoryId,
                                                     @RequestParam(required = false) String startTime,
                                                     @RequestParam(required = false) String endTime) {
        IPage<RiceMillingBatch> result = millingBatchService
                .page(pageNo, pageSize, grainBatchId, productBatchId, factoryId, startTime, endTime);
        return Result.success(PageResult.of(result));
    }

    /** 查询加工详情 */
    @GetMapping("/{millingBatchId}")
    @RequirePermission(PermissionConstants.RICE_MILLING_VIEW)
    public Result<RiceMillingBatch> detail(@PathVariable String millingBatchId) {
        return Result.success(millingBatchService.detail(millingBatchId));
    }

    /** 完成加工并计算产出率 */
    @PutMapping("/{millingBatchId}/complete")
    @RequirePermission(PermissionConstants.RICE_MILLING_UPDATE)
    public Result<RiceMillingBatch> complete(@PathVariable String millingBatchId,
                                             @Valid @RequestBody MillingBatchCompleteDTO dto,
                                             HttpServletRequest request) {
        return Result.success(millingBatchService.complete(millingBatchId, dto, request.getRemoteAddr()));
    }
}
