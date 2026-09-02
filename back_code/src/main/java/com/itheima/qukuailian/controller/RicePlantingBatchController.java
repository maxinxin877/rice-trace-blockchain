package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.PlantingBatchCreateDTO;
import com.itheima.qukuailian.dto.PlantingBatchUpdateDTO;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.service.RicePlantingBatchService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 种植批次接口（接口文档 v1.1 §5.6-§5.9）
 */
@RestController
@RequestMapping("/rice/planting-batches")
@RequiredArgsConstructor
public class RicePlantingBatchController {

    private final RicePlantingBatchService plantingBatchService;

    /** 创建种植批次 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_PLANTING_BATCH_CREATE)
    public Result<RicePlantingBatch> create(@Valid @RequestBody PlantingBatchCreateDTO dto,
                                            HttpServletRequest request) {
        return Result.success(plantingBatchService.create(dto, request.getRemoteAddr()));
    }

    /** 分页查询种植批次 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_PLANTING_BATCH_VIEW)
    public Result<PageResult<RicePlantingBatch>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                      @RequestParam(defaultValue = "20") long pageSize,
                                                      @RequestParam(required = false) String fieldId,
                                                      @RequestParam(required = false) String riceVariety,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String sowingDateStart,
                                                      @RequestParam(required = false) String sowingDateEnd) {
        IPage<RicePlantingBatch> result = plantingBatchService
                .page(pageNo, pageSize, fieldId, riceVariety, status, sowingDateStart, sowingDateEnd);
        return Result.success(PageResult.of(result));
    }

    /** 查询种植批次详情 */
    @GetMapping("/{plantingBatchId}")
    @RequirePermission(PermissionConstants.RICE_PLANTING_BATCH_VIEW)
    public Result<RicePlantingBatch> detail(@PathVariable String plantingBatchId) {
        return Result.success(plantingBatchService.detail(plantingBatchId));
    }

    /** 更新种植批次（完整覆盖 + 审计原因） */
    @PutMapping("/{plantingBatchId}")
    @RequirePermission(PermissionConstants.RICE_PLANTING_BATCH_UPDATE)
    public Result<RicePlantingBatch> update(@PathVariable String plantingBatchId,
                                            @Valid @RequestBody PlantingBatchUpdateDTO dto,
                                            HttpServletRequest request) {
        return Result.success(plantingBatchService.update(plantingBatchId, dto, request.getRemoteAddr()));
    }
}
