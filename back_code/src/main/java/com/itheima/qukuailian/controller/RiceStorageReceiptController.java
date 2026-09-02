package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.QualityTestSubmitDTO;
import com.itheima.qukuailian.dto.StorageReceiptCreateDTO;
import com.itheima.qukuailian.entity.RiceQualityTest;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.service.RiceStorageReceiptService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 收储入库接口（接口文档 v1.1 §5.14-§5.17）
 */
@RestController
@RequestMapping("/rice/storage-receipts")
@RequiredArgsConstructor
public class RiceStorageReceiptController {

    private final RiceStorageReceiptService storageReceiptService;

    /** 创建入库单 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_STORAGE_CREATE)
    public Result<RiceStorageReceipt> create(@Valid @RequestBody StorageReceiptCreateDTO dto,
                                             HttpServletRequest request) {
        return Result.success(storageReceiptService.create(dto, request.getRemoteAddr()));
    }

    /** 分页查询入库单 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_STORAGE_VIEW)
    public Result<PageResult<RiceStorageReceipt>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                       @RequestParam(defaultValue = "20") long pageSize,
                                                       @RequestParam(required = false) String grainBatchId,
                                                       @RequestParam(required = false) String plantingBatchId,
                                                       @RequestParam(required = false) String warehouseId,
                                                       @RequestParam(required = false) String grainGrade,
                                                       @RequestParam(required = false) String startTime,
                                                       @RequestParam(required = false) String endTime) {
        IPage<RiceStorageReceipt> result = storageReceiptService
                .page(pageNo, pageSize, grainBatchId, plantingBatchId, warehouseId, grainGrade, startTime, endTime);
        return Result.success(PageResult.of(result));
    }

    /** 查询入库详情 */
    @GetMapping("/{storageReceiptId}")
    @RequirePermission(PermissionConstants.RICE_STORAGE_VIEW)
    public Result<RiceStorageReceipt> detail(@PathVariable String storageReceiptId) {
        return Result.success(storageReceiptService.detail(storageReceiptId));
    }

    /** 提交入库质检 */
    @PostMapping("/{storageReceiptId}/quality-tests")
    @RequirePermission(PermissionConstants.RICE_STORAGE_QUALITY_CREATE)
    public Result<RiceQualityTest> submitQualityTest(@PathVariable String storageReceiptId,
                                                     @Valid @RequestBody QualityTestSubmitDTO dto) {
        return Result.success(storageReceiptService.submitQualityTest(storageReceiptId, dto));
    }
}
