package com.itheima.qukuailian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.common.PageQuery;
import com.itheima.qukuailian.common.PageResult;
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

    /** 分页查询链上存证列表（管理端"链上核验"页面） */
    @GetMapping
    @RequirePermission(PermissionConstants.RICE_CHAIN_PROOF_VIEW)
    public Result<PageResult<RiceChainProof>> page(PageQuery pageQuery,
                                                   @RequestParam(required = false) String businessType,
                                                   @RequestParam(required = false) String businessId,
                                                   @RequestParam(required = false) String chainStatus) {
        IPage<RiceChainProof> result = chainProofService.page(pageQuery.resolvePageNo(), pageQuery.resolvePageSize(),
                businessType, businessId, chainStatus);
        return Result.success(PageResult.of(result));
    }

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
