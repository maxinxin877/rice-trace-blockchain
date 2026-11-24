package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.service.RiceComplianceGuardService;
import com.itheima.qukuailian.utils.PermissionConstants;
import com.itheima.qukuailian.vo.ComplianceCheckVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 生产防错校验接口（需求文档 §8.4、§九 FC-01~FC-05）
 * <p>前端在"提交入库 / 开始加工 / 赋码激活"前先调一次，把红黄绿灯提前展示出来，
 * 避免用户填完表单才被后端 42201 打回。</p>
 */
@RestController
@RequestMapping("/rice/compliance")
@RequiredArgsConstructor
public class RiceComplianceController {

    private final RiceComplianceGuardService complianceGuardService;

    /**
     * 执行防错校验
     *
     * @param businessType PLANTING_BATCH/GRAIN_BATCH/STORAGE_RECEIPT/PRODUCT_BATCH/TRACE_CODE
     * @param businessId   业务 ID
     * @param action       STORE/MILL/ACTIVATE/SELL
     */
    @GetMapping("/check")
    @RequirePermission(PermissionConstants.RICE_COMPLIANCE_VIEW)
    public Result<List<ComplianceCheckVO>> check(@RequestParam String businessType,
                                                 @RequestParam String businessId,
                                                 @RequestParam String action) {
        return Result.success(complianceGuardService.check(businessType, businessId, action));
    }
}
