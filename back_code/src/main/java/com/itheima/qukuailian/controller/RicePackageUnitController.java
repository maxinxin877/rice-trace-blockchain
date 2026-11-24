package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.PackageUnitCreateDTO;
import com.itheima.qukuailian.entity.RicePackageUnit;
import com.itheima.qukuailian.service.RicePackageUnitService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 箱码 / 托盘码接口（需求文档 §九 YM-01 统一编码体系）
 */
@RestController
@RequestMapping("/rice/packages")
@RequiredArgsConstructor
public class RicePackageUnitController {

    private final RicePackageUnitService packageUnitService;

    /** 创建箱码或托盘码并绑定子码 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_PACKAGE_CREATE)
    public Result<RicePackageUnit> create(@Valid @RequestBody PackageUnitCreateDTO dto,
                                          HttpServletRequest request) {
        return Result.success(packageUnitService.create(dto, request.getRemoteAddr()));
    }

    /** 分页查询 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_PACKAGE_VIEW)
    public Result<PageResult<RicePackageUnit>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                    @RequestParam(defaultValue = "20") long pageSize,
                                                    @RequestParam(required = false) String unitLevel,
                                                    @RequestParam(required = false) String productBatchId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String unitCode) {
        IPage<RicePackageUnit> result = packageUnitService.page(pageNo, pageSize, unitLevel, productBatchId,
                status, unitCode);
        return Result.success(PageResult.of(result));
    }

    /** 详情：单元 + 子码 + 展开后的单品码 */
    @GetMapping("/{unitCode}")
    @RequirePermission(PermissionConstants.RICE_PACKAGE_VIEW)
    public Result<Map<String, Object>> detail(@PathVariable String unitCode) {
        return Result.success(packageUnitService.detail(unitCode));
    }

    /** 把任意层级展开为单品码清单 */
    @GetMapping("/{unitCode}/trace-codes")
    @RequirePermission(PermissionConstants.RICE_PACKAGE_VIEW)
    public Result<List<String>> expand(@PathVariable String unitCode) {
        return Result.success(packageUnitService.expandTraceCodes(unitCode));
    }

    /** 单品码向上追溯所属箱码与托盘码 */
    @GetMapping("/trace-code/{traceCode}/upward")
    @RequirePermission(PermissionConstants.RICE_PACKAGE_VIEW)
    public Result<Map<String, Object>> upward(@PathVariable String traceCode) {
        return Result.success(packageUnitService.traceUpward(traceCode));
    }
}
