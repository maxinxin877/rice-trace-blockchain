package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageQuery;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.FieldCreateDTO;
import com.itheima.qukuailian.dto.FieldPhotoBindDTO;
import com.itheima.qukuailian.dto.FieldUpdateDTO;
import com.itheima.qukuailian.entity.RiceField;
import com.itheima.qukuailian.service.RiceFieldService;
import com.itheima.qukuailian.utils.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 地块档案接口（接口文档 v1.1 §5.1-§5.5）
 * <p>分页参数同时兼容 pageNo（文档规范）与 page（前端常用写法）。</p>
 */
@RestController
@RequestMapping("/rice/fields")
@RequiredArgsConstructor
public class RiceFieldController {

    private final RiceFieldService riceFieldService;

    /** 创建地块 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_FIELD_CREATE)
    public Result<RiceField> create(@Valid @RequestBody FieldCreateDTO dto, HttpServletRequest request) {
        return Result.success(riceFieldService.create(dto, request.getRemoteAddr()));
    }

    /** 分页查询地块（支持 chainStatus 链上状态筛选） */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_FIELD_VIEW)
    public Result<PageResult<RiceField>> page(PageQuery pageQuery,
                                              @RequestParam(required = false) String fieldCode,
                                              @RequestParam(required = false) String fieldName,
                                              @RequestParam(required = false) String farmerName,
                                              @RequestParam(required = false) String district,
                                              @RequestParam(required = false) String chainStatus) {
        IPage<RiceField> result = riceFieldService.page(pageQuery.resolvePageNo(), pageQuery.resolvePageSize(),
                fieldCode, fieldName, farmerName, district, chainStatus);
        return Result.success(PageResult.of(result));
    }

    /** 查询地块详情（含在种批次摘要） */
    @GetMapping("/{fieldId}")
    @RequirePermission(PermissionConstants.RICE_FIELD_VIEW)
    public Result<RiceField> detail(@PathVariable String fieldId) {
        return Result.success(riceFieldService.detail(fieldId));
    }

    /** 更新地块（完整覆盖 + 审计原因） */
    @PutMapping("/{fieldId}")
    @RequirePermission(PermissionConstants.RICE_FIELD_UPDATE)
    public Result<RiceField> update(@PathVariable String fieldId,
                                    @Valid @RequestBody FieldUpdateDTO dto,
                                    HttpServletRequest request) {
        return Result.success(riceFieldService.update(fieldId, dto, request.getRemoteAddr()));
    }

    /** 绑定基地实景照片 */
    @PostMapping("/{fieldId}/photos")
    @RequirePermission(PermissionConstants.RICE_FIELD_UPDATE)
    public Result<Map<String, Object>> bindPhotos(@PathVariable String fieldId,
                                                  @Valid @RequestBody FieldPhotoBindDTO dto,
                                                  HttpServletRequest request) {
        riceFieldService.bindPhotos(fieldId, dto, request.getRemoteAddr());
        return Result.success(Map.of("fieldId", fieldId, "basePhotoFileIds", dto.getFileIds()));
    }

    /** 删除地块（存在种植批次时不允许删除） */
    @DeleteMapping("/{fieldId}")
    @RequirePermission(PermissionConstants.RICE_FIELD_UPDATE)
    public Result<Void> delete(@PathVariable String fieldId, HttpServletRequest request) {
        riceFieldService.delete(fieldId, request.getRemoteAddr());
        return Result.success();
    }
}
