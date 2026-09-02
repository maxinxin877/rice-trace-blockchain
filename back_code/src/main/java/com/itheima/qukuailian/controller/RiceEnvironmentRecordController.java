package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.EnvironmentRecordCreateDTO;
import com.itheima.qukuailian.entity.RiceEnvironmentRecord;
import com.itheima.qukuailian.service.RiceEnvironmentRecordService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 稻田环境数据接口（接口文档 v1.1 §5.12-§5.13）
 */
@RestController
@RequestMapping("/rice/planting-batches/{plantingBatchId}/environment-records")
@RequiredArgsConstructor
public class RiceEnvironmentRecordController {

    private final RiceEnvironmentRecordService environmentRecordService;

    /** 上传稻田环境数据 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_ENV_RECORD_CREATE)
    public Result<RiceEnvironmentRecord> create(@PathVariable String plantingBatchId,
                                                @Valid @RequestBody EnvironmentRecordCreateDTO dto) {
        return Result.success(environmentRecordService.create(plantingBatchId, dto));
    }

    /** 查询稻田环境数据 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_ENV_RECORD_VIEW)
    public Result<PageResult<RiceEnvironmentRecord>> page(@PathVariable String plantingBatchId,
                                                          @RequestParam(required = false) String sourceType,
                                                          @RequestParam(required = false) String startTime,
                                                          @RequestParam(required = false) String endTime,
                                                          @RequestParam(defaultValue = "1") long pageNo,
                                                          @RequestParam(defaultValue = "20") long pageSize) {
        IPage<RiceEnvironmentRecord> result = environmentRecordService
                .page(plantingBatchId, sourceType, startTime, endTime, pageNo, pageSize);
        return Result.success(PageResult.of(result));
    }
}
