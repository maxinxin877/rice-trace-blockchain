package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.service.RiceProductBatchService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 成品米批次接口（接口文档 v1.1 §5.22-§5.23）
 */
@RestController
@RequestMapping("/rice/product-batches")
@RequiredArgsConstructor
public class RiceProductBatchController {

    private final RiceProductBatchService productBatchService;

    /** 创建成品米批次 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_PRODUCT_BATCH_CREATE)
    public Result<RiceProductBatch> create(@Valid @RequestBody ProductBatchCreateDTO dto) {
        return Result.success(productBatchService.create(dto));
    }

    /** 分页查询成品米批次 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_PRODUCT_BATCH_VIEW)
    public Result<PageResult<RiceProductBatch>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                     @RequestParam(defaultValue = "20") long pageSize,
                                                     @RequestParam(required = false) String productBatchId,
                                                     @RequestParam(required = false) String productName,
                                                     @RequestParam(required = false) String brandName,
                                                     @RequestParam(required = false) String status) {
        IPage<RiceProductBatch> result = productBatchService
                .page(pageNo, pageSize, productBatchId, productName, brandName, status);
        return Result.success(PageResult.of(result));
    }
}
