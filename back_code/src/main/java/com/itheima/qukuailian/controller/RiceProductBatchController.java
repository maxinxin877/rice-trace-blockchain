package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageQuery;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.service.RiceProductBatchService;
import com.itheima.qukuailian.utils.PermissionConstants;
import com.itheima.qukuailian.vo.ProductTraceVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 成品米批次接口（接口文档 v1.1 §5.22-§5.23）
 * <p>分页参数同时兼容 pageNo（文档规范）与 page（前端常用写法）。</p>
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
    public Result<PageResult<RiceProductBatch>> page(PageQuery pageQuery,
                                                     @RequestParam(required = false) String productBatchId,
                                                     @RequestParam(required = false) String productName,
                                                     @RequestParam(required = false) String brandName,
                                                     @RequestParam(required = false) String riceVariety,
                                                     @RequestParam(required = false) String status) {
        IPage<RiceProductBatch> result = productBatchService.page(pageQuery.resolvePageNo(), pageQuery.resolvePageSize(),
                productBatchId, productName, brandName, status, riceVariety);
        return Result.success(PageResult.of(result));
    }

    /** 成品批次详情 */
    @GetMapping("/{productBatchId}")
    @RequirePermission(PermissionConstants.RICE_PRODUCT_BATCH_VIEW)
    public Result<RiceProductBatch> detail(@PathVariable String productBatchId) {
        return Result.success(productBatchService.detail(productBatchId));
    }

    /** 更新成品批次 */
    @PutMapping("/{productBatchId}")
    @RequirePermission(PermissionConstants.RICE_PRODUCT_BATCH_CREATE)
    public Result<RiceProductBatch> update(@PathVariable String productBatchId,
                                           @Valid @RequestBody ProductBatchCreateDTO dto,
                                           @RequestParam(required = false) String reason,
                                           HttpServletRequest request) {
        return Result.success(productBatchService.update(productBatchId, dto, reason, request.getRemoteAddr()));
    }

    /** 成品批次溯源链路：成品 → 加工 → 入库 → 种植 → 地块 + 防伪码 */
    @GetMapping("/{productBatchId}/trace")
    @RequirePermission(PermissionConstants.RICE_PRODUCT_BATCH_VIEW)
    public Result<ProductTraceVO> trace(@PathVariable String productBatchId) {
        return Result.success(productBatchService.trace(productBatchId));
    }
}
