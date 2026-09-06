package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.FarmingLogCreateDTO;
import com.itheima.qukuailian.entity.RiceFarmingLog;
import com.itheima.qukuailian.service.RiceFarmingLogService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 农事记录接口（接口文档 v1.1 §5.10-§5.11）
 */
@RestController
@RequestMapping("/rice/planting-batches/{plantingBatchId}/farming-logs")
@RequiredArgsConstructor
public class RiceFarmingLogController {

    private final RiceFarmingLogService farmingLogService;

    /** 新增农事记录（上链） */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_FARMING_LOG_CREATE)
    public Result<RiceFarmingLog> create(@PathVariable String plantingBatchId,
                                         @Valid @RequestBody FarmingLogCreateDTO dto) {
        return Result.success(farmingLogService.create(plantingBatchId, dto));
    }

    /** 查询农事记录 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_FARMING_LOG_VIEW)
    public Result<PageResult<RiceFarmingLog>> page(@PathVariable String plantingBatchId,
                                                   @RequestParam(required = false) String operationType,
                                                   @RequestParam(required = false) String startTime,
                                                   @RequestParam(required = false) String endTime,
                                                   @RequestParam(defaultValue = "1") long pageNo,
                                                   @RequestParam(defaultValue = "20") long pageSize) {
        IPage<RiceFarmingLog> result = farmingLogService
                .page(plantingBatchId, operationType, startTime, endTime, pageNo, pageSize);
        return Result.success(PageResult.of(result));
    }
}
