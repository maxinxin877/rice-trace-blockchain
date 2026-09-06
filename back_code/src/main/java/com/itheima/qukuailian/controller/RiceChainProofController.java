package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.common.annotation.RequirePermission;
import com.itheima.qukuailian.entity.RiceChainProof;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.utils.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 链上存证接口（接口文档 v1.1 §7.6-§7.7）
 */
@RestController
@RequestMapping("/rice/chain-proofs")
@RequiredArgsConstructor
public class RiceChainProofController {

    private final ChainProofService chainProofService;

    /** 查询业务对象链上存证 */
    @GetMapping("/{businessType}/{businessId}")
    @RequirePermission(PermissionConstants.RICE_CHAIN_PROOF_VIEW)
    public Result<RiceChainProof> getProof(@PathVariable String businessType,
                                           @PathVariable String businessId) {
        return Result.success(chainProofService.getProof(businessType, businessId));
    }

    /** 核验业务对象链上存证 */
    @PostMapping("/{businessType}/{businessId}/verify")
    @RequirePermission(PermissionConstants.RICE_CHAIN_PROOF_VERIFY)
    public Result<Map<String, Object>> verify(@PathVariable String businessType,
                                              @PathVariable String businessId) {
        return Result.success(chainProofService.verify(businessType, businessId));
    }
}
