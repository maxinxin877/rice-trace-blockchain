package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.service.RiceTraceCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消费者扫码溯源（小程序/H5 公开接口，免登录，拦截器不拦截 /mini/**）
 */
@RestController
@RequestMapping("/mini/traces")
@RequiredArgsConstructor
public class MiniTraceController {

    private final RiceTraceCodeService traceCodeService;

    /** 按防伪码查询全链路溯源详情；码不存在时 data 返回 null，由前端展示空状态 */
    @GetMapping("/{traceCode}")
    public Result<Map<String, Object>> trace(@PathVariable String traceCode) {
        return Result.success(traceCodeService.traceDetail(traceCode));
    }

    /** 按防伪码查询链上存证（消费者公开核验） */
    @GetMapping("/{traceCode}/chain-proof")
    public Result<Map<String, Object>> chainProof(@PathVariable String traceCode) {
        return Result.success(traceCodeService.chainProof(traceCode));
    }

    /** 重新计算摘要并核验（公开接口，核验逻辑与查询一致） */
    @PostMapping("/{traceCode}/chain-proof/verify")
    public Result<Map<String, Object>> verifyChainProof(@PathVariable String traceCode) {
        return Result.success(traceCodeService.chainProof(traceCode));
    }

    /** 消费者扫码验真（公开接口，body: {"region": "北京市"}） */
    @PostMapping("/{traceCode}/verify")
    public Result<Map<String, Object>> verifyCode(@PathVariable String traceCode,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String region = body == null ? null : body.get("region");
        return Result.success(traceCodeService.verifyCode(traceCode, region));
    }
}
