package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.dto.CollectBatchDTO;
import com.itheima.qukuailian.dto.CollectDTO;
import com.itheima.qukuailian.dto.CollectReviewDTO;
import com.itheima.qukuailian.entity.RiceCollectRecord;
import com.itheima.qukuailian.service.RiceCollectService;
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
 * 扫码采集接口（需求文档 §九 CJ-01/02/05/06/07）
 * <p>扫码枪、PDA、移动端、Web 手工录入共用这一组接口，终端只负责传码 + 动作，识别与校验在服务端统一完成。</p>
 */
@RestController
@RequestMapping("/rice/collect")
@RequiredArgsConstructor
public class RiceCollectController {

    private final RiceCollectService collectService;

    /** 单条采集：自动识别码类型、带出业务数据、返回防错校验结果 */
    @PostMapping
    @RequirePermission(PermissionConstants.RICE_COLLECT_CREATE)
    public Result<RiceCollectRecord> collect(@Valid @RequestBody CollectDTO dto, HttpServletRequest request) {
        return Result.success(collectService.collect(dto, request.getRemoteAddr()));
    }

    /** 离线补传：逐条幂等入库，返回 accepted/duplicated/failed 统计 */
    @PostMapping("/batch")
    @RequirePermission(PermissionConstants.RICE_COLLECT_CREATE)
    public Result<Map<String, Object>> batchCollect(@Valid @RequestBody CollectBatchDTO dto,
                                                    HttpServletRequest request) {
        return Result.success(collectService.batchCollect(dto, request.getRemoteAddr()));
    }

    /** 人工补录审核 */
    @PostMapping("/{collectId}/review")
    @RequirePermission(PermissionConstants.RICE_COLLECT_REVIEW)
    public Result<RiceCollectRecord> review(@PathVariable String collectId,
                                            @Valid @RequestBody CollectReviewDTO dto,
                                            HttpServletRequest request) {
        return Result.success(collectService.review(collectId, dto, request.getRemoteAddr()));
    }

    /** 采集记录分页查询 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_COLLECT_VIEW)
    public Result<PageResult<RiceCollectRecord>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                      @RequestParam(defaultValue = "20") long pageSize,
                                                      @RequestParam(required = false) String code,
                                                      @RequestParam(required = false) String action,
                                                      @RequestParam(required = false) String terminalType,
                                                      @RequestParam(required = false) String source,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String checkResult,
                                                      @RequestParam(required = false) String startTime,
                                                      @RequestParam(required = false) String endTime) {
        IPage<RiceCollectRecord> result = collectService.page(pageNo, pageSize, code, action, terminalType,
                source, status, checkResult, startTime, endTime);
        return Result.success(PageResult.of(result));
    }

    /** 采集概览 */
    @GetMapping("/stats")
    @RequirePermission(PermissionConstants.RICE_COLLECT_VIEW)
    public Result<Map<String, Object>> stats() {
        return Result.success(collectService.stats());
    }
}
