package com.itheima.qukuailian.service;

import com.itheima.qukuailian.vo.MiniChainProofVO;
import com.itheima.qukuailian.vo.MiniTraceDetailVO;
import com.itheima.qukuailian.vo.MiniVerifyResultVO;

import java.math.BigDecimal;

/**
 * 小程序扫码溯源服务（接口文档 v1.1 §6.2-§6.4，公开接口）
 */
public interface RiceMiniTraceService {

    /** 扫码溯源详情（不增加扫码次数） */
    MiniTraceDetailVO getTraceDetail(String traceCode);

    /** 真伪鉴别：记录扫码日志、更新扫码次数、按规则生成风险预警 */
    MiniVerifyResultVO verify(String traceCode, BigDecimal lng, BigDecimal lat,
                              String region, String deviceId, String scene);

    /** 链上核验结果 */
    MiniChainProofVO chainProof(String traceCode);
}
