package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.dto.MiniVerifyDTO;
import com.itheima.qukuailian.service.RiceMiniTraceService;
import com.itheima.qukuailian.vo.MiniChainProofVO;
import com.itheima.qukuailian.vo.MiniTraceDetailVO;
import com.itheima.qukuailian.vo.MiniVerifyResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 小程序端扫码溯源接口（接口文档 v1.1 §6.2-§6.4，公开接口，无需登录）
 * <ul>
 *   <li>GET  /api/v1/mini/rice/trace/{traceCode}               扫码溯源详情</li>
 *   <li>POST /api/v1/mini/rice/trace/{traceCode}/verify        真伪鉴别（记录扫码）</li>
 *   <li>GET  /api/v1/mini/rice/trace/{traceCode}/chain-proof   链上核验结果</li>
 * </ul>
 */
@RestController
@RequestMapping("/mini/rice/trace")
@RequiredArgsConstructor
public class RiceMiniTraceController {

    private final RiceMiniTraceService miniTraceService;

    /** 扫码溯源详情：不增加扫码次数 */
    @GetMapping("/{traceCode}")
    public Result<MiniTraceDetailVO> traceDetail(@PathVariable String traceCode) {
        return Result.success(miniTraceService.getTraceDetail(traceCode));
    }

    /** 真伪鉴别：记录扫码日志并返回结果（支持 JSON 体或查询参数两种传参） */
    @PostMapping("/{traceCode}/verify")
    public Result<MiniVerifyResultVO> verify(@PathVariable String traceCode,
                                             @RequestBody(required = false) MiniVerifyDTO body,
                                             @RequestParam(required = false) BigDecimal lng,
                                             @RequestParam(required = false) BigDecimal lat,
                                             @RequestParam(required = false) String region,
                                             @RequestParam(required = false) String deviceId,
                                             @RequestParam(required = false) String scene,
                                             @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader) {
        MiniVerifyDTO dto = body == null ? new MiniVerifyDTO() : body;
        BigDecimal effectiveLng = dto.getLng() != null ? dto.getLng() : lng;
        BigDecimal effectiveLat = dto.getLat() != null ? dto.getLat() : lat;
        String effectiveRegion = dto.getRegion() != null ? dto.getRegion() : region;
        String effectiveDeviceId = dto.getDeviceId() != null ? dto.getDeviceId()
                : (deviceId != null ? deviceId : deviceIdHeader);
        String effectiveScene = dto.getScene() != null ? dto.getScene() : scene;
        return Result.success(miniTraceService.verify(traceCode, effectiveLng, effectiveLat,
                effectiveRegion, effectiveDeviceId, effectiveScene));
    }

    /** 链上核验结果 */
    @GetMapping("/{traceCode}/chain-proof")
    public Result<MiniChainProofVO> chainProof(@PathVariable String traceCode) {
        return Result.success(miniTraceService.chainProof(traceCode));
    }

    /** 重新核验链上存证（小程序"重新计算并核验"按钮） */
    @PostMapping("/{traceCode}/chain-proof")
    public Result<MiniChainProofVO> reVerifyChainProof(@PathVariable String traceCode) {
        return Result.success(miniTraceService.chainProof(traceCode));
    }
}
