package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageResult;
import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.service.RiceTraceCodeService;
import com.itheima.qukuailian.utils.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 窜货预警接口（接口文档 v1.1 §5.27）
 * <p>预警由扫码事件生成（小程序真伪鉴别流程），本接口负责查询展示。</p>
 */
@RestController
@RequestMapping("/rice/channel-warnings")
@RequiredArgsConstructor
public class RiceChannelWarningController {

    private final RiceTraceCodeService traceCodeService;

    /** 查询窜货预警 */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_CHANNEL_WARNING_VIEW)
    public Result<PageResult<RiceRiskWarning>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                    @RequestParam(defaultValue = "20") long pageSize,
                                                    @RequestParam(required = false) String productBatchId,
                                                    @RequestParam(required = false) String traceCode,
                                                    @RequestParam(required = false) String riskLevel,
                                                    @RequestParam(required = false) String region,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime) {
        IPage<RiceRiskWarning> result = traceCodeService.pageChannelWarnings(
                pageNo, pageSize, productBatchId, traceCode, riskLevel, region, startTime, endTime);
        return Result.success(PageResult.of(result));
    }
}
