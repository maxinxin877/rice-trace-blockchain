package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.RecallApproveDTO;
import com.itheima.qukuailian.dto.RecallCloseDTO;
import com.itheima.qukuailian.dto.RecallCreateDTO;
import com.itheima.qukuailian.dto.RecallItemHandleDTO;
import com.itheima.qukuailian.dto.RecallReleaseDTO;
import com.itheima.qukuailian.entity.RiceRecallItem;
import com.itheima.qukuailian.entity.RiceRecallTask;
import com.itheima.qukuailian.service.RiceRecallService;
import com.itheima.qukuailian.utils.PermissionConstants;
import com.itheima.qukuailian.vo.TraceImpactVO;
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

/**
 * 召回管理接口（需求文档 §六-4、§九 FC-05/FC-06/JK-06）
 */
@RestController
@RequestMapping("/rice/recalls")
@RequiredArgsConstructor
public class RiceRecallController {

    private final RiceRecallService recallService;

    /** 影响范围预览：创建召回前先看会波及哪些成品/防伪码/区域/重量 */
    @GetMapping("/scope-preview")
    @RequirePermission(PermissionConstants.RICE_RECALL_VIEW)
    public Result<TraceImpactVO> previewScope(@RequestParam String businessType,
                                              @RequestParam String businessId) {
        return Result.success(recallService.previewScope(businessType, businessId));
    }

    /** 创建召回任务（DRAFT） */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_RECALL_CREATE)
    public Result<RiceRecallTask> create(@Valid @RequestBody RecallCreateDTO dto, HttpServletRequest request) {
        return Result.success(recallService.create(dto, request.getRemoteAddr()));
    }

    /** 提交审批 */
    @PostMapping("/{recallTaskId}/submit")
    @RequirePermission(PermissionConstants.RICE_RECALL_CREATE)
    public Result<RiceRecallTask> submit(@PathVariable String recallTaskId, HttpServletRequest request) {
        return Result.success(recallService.submit(recallTaskId, request.getRemoteAddr()));
    }

    /** 审批通过 */
    @PostMapping("/{recallTaskId}/approve")
    @RequirePermission(PermissionConstants.RICE_RECALL_APPROVE)
    public Result<RiceRecallTask> approve(@PathVariable String recallTaskId,
                                          @RequestBody(required = false) RecallApproveDTO dto,
                                          HttpServletRequest request) {
        return Result.success(recallService.approve(recallTaskId, dto, request.getRemoteAddr()));
    }

    /** 审批驳回 */
    @PostMapping("/{recallTaskId}/reject")
    @RequirePermission(PermissionConstants.RICE_RECALL_APPROVE)
    public Result<RiceRecallTask> reject(@PathVariable String recallTaskId,
                                         @RequestBody(required = false) RecallApproveDTO dto,
                                         HttpServletRequest request) {
        return Result.success(recallService.reject(recallTaskId, dto, request.getRemoteAddr()));
    }

    /** 发布召回：冻结批次与防伪码、下发通知、指令上链 */
    @PostMapping("/{recallTaskId}/release")
    @RequirePermission(PermissionConstants.RICE_RECALL_APPROVE)
    public Result<RiceRecallTask> release(@PathVariable String recallTaskId,
                                          @RequestBody(required = false) RecallReleaseDTO dto,
                                          HttpServletRequest request) {
        return Result.success(recallService.release(recallTaskId, dto, request.getRemoteAddr()));
    }

    /** 登记单条明细的处置结果（下架/回收/隔离/销毁/未召回） */
    @PostMapping("/items/{itemId}/handle")
    @RequirePermission(PermissionConstants.RICE_RECALL_HANDLE)
    public Result<RiceRecallItem> handleItem(@PathVariable String itemId,
                                             @Valid @RequestBody RecallItemHandleDTO dto,
                                             HttpServletRequest request) {
        return Result.success(recallService.handleItem(itemId, dto, request.getRemoteAddr()));
    }

    /** 关闭召回（需给出处置结论） */
    @PostMapping("/{recallTaskId}/close")
    @RequirePermission(PermissionConstants.RICE_RECALL_APPROVE)
    public Result<RiceRecallTask> close(@PathVariable String recallTaskId,
                                        @Valid @RequestBody RecallCloseDTO dto,
                                        HttpServletRequest request) {
        return Result.success(recallService.close(recallTaskId, dto, request.getRemoteAddr()));
    }

    /** 取消召回（未发布前） */
    @PostMapping("/{recallTaskId}/cancel")
    @RequirePermission(PermissionConstants.RICE_RECALL_CREATE)
    public Result<RiceRecallTask> cancel(@PathVariable String recallTaskId,
                                         @RequestParam(required = false) String reason,
                                         HttpServletRequest request) {
        return Result.success(recallService.cancel(recallTaskId, reason, request.getRemoteAddr()));
    }

    /** 分页查询召回任务 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_RECALL_VIEW)
    public Result<PageResult<RiceRecallTask>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                   @RequestParam(defaultValue = "20") long pageSize,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String riskLevel,
                                                   @RequestParam(required = false) String triggerType,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String startTime,
                                                   @RequestParam(required = false) String endTime) {
        IPage<RiceRecallTask> result = recallService.page(pageNo, pageSize, status, riskLevel,
                triggerType, keyword, startTime, endTime);
        return Result.success(PageResult.of(result));
    }

    /** 召回详情：任务 + 明细 + 处置进度 + 链上存证 */
    @GetMapping("/{recallTaskId}")
    @RequirePermission(PermissionConstants.RICE_RECALL_VIEW)
    public Result<RiceRecallTask> detail(@PathVariable String recallTaskId) {
        return Result.success(recallService.detail(recallTaskId));
    }
}
