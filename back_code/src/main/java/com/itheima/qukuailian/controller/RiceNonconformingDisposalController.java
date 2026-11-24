package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.DisposalCreateDTO;
import com.itheima.qukuailian.dto.DisposalReviewDTO;
import com.itheima.qukuailian.entity.RiceNonconformingDisposal;
import com.itheima.qukuailian.service.RiceNonconformingDisposalService;
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

import java.util.Map;

/**
 * 不合格品处置接口（需求文档 §九 FC-04）
 */
@RestController
@RequestMapping("/rice/quality/disposals")
@RequiredArgsConstructor
public class RiceNonconformingDisposalController {

    private final RiceNonconformingDisposalService disposalService;

    /** 创建处置单 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_DISPOSAL_CREATE)
    public Result<RiceNonconformingDisposal> create(@Valid @RequestBody DisposalCreateDTO dto,
                                                    HttpServletRequest request) {
        return Result.success(disposalService.create(dto, request.getRemoteAddr()));
    }

    /** 提交评审 */
    @PostMapping("/{disposalId}/submit")
    @RequirePermission(PermissionConstants.RICE_DISPOSAL_CREATE)
    public Result<RiceNonconformingDisposal> submit(@PathVariable String disposalId,
                                                    HttpServletRequest request) {
        return Result.success(disposalService.submit(disposalId, request.getRemoteAddr()));
    }

    /** 评审（通过即隔离） */
    @PostMapping("/{disposalId}/review")
    @RequirePermission(PermissionConstants.RICE_DISPOSAL_REVIEW)
    public Result<RiceNonconformingDisposal> review(@PathVariable String disposalId,
                                                    @Valid @RequestBody DisposalReviewDTO dto,
                                                    HttpServletRequest request) {
        return Result.success(disposalService.review(disposalId, dto, request.getRemoteAddr()));
    }

    /** 执行处置（返工/报废/让步接收等） */
    @PostMapping("/{disposalId}/execute")
    @RequirePermission(PermissionConstants.RICE_DISPOSAL_REVIEW)
    public Result<RiceNonconformingDisposal> execute(@PathVariable String disposalId,
                                                     @RequestBody(required = false) Map<String, String> body,
                                                     HttpServletRequest request) {
        String remark = body == null ? "" : body.getOrDefault("remark", "");
        return Result.success(disposalService.execute(disposalId, remark, request.getRemoteAddr()));
    }

    /** 关闭处置单 */
    @PostMapping("/{disposalId}/close")
    @RequirePermission(PermissionConstants.RICE_DISPOSAL_REVIEW)
    public Result<RiceNonconformingDisposal> close(@PathVariable String disposalId,
                                                   @RequestBody(required = false) Map<String, String> body,
                                                   HttpServletRequest request) {
        String remark = body == null ? "" : body.getOrDefault("remark", "");
        return Result.success(disposalService.close(disposalId, remark, request.getRemoteAddr()));
    }

    /** 分页查询 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_DISPOSAL_VIEW)
    public Result<PageResult<RiceNonconformingDisposal>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                              @RequestParam(defaultValue = "20") long pageSize,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String disposition,
                                                              @RequestParam(required = false) String businessType,
                                                              @RequestParam(required = false) String businessId,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String riskLevel,
                                                              @RequestParam(required = false) String startTime,
                                                              @RequestParam(required = false) String endTime) {
        IPage<RiceNonconformingDisposal> result = disposalService.page(pageNo, pageSize, status, disposition,
                businessType, businessId, keyword, riskLevel, startTime, endTime);
        return Result.success(PageResult.of(result));
    }

    /** 详情 */
    @GetMapping("/{disposalId}")
    @RequirePermission(PermissionConstants.RICE_DISPOSAL_VIEW)
    public Result<RiceNonconformingDisposal> detail(@PathVariable String disposalId) {
        return Result.success(disposalService.detail(disposalId));
    }
}
