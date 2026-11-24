package com.itheima.qukuailian.service;

import com.itheima.qukuailian.vo.ComplianceCheckVO;

import java.util.List;

/**
 * 生产防错校验服务（需求文档 §8.4 生产防错体系、§九 FC-01~FC-05）
 * <p>核心思想：把"能不能继续往下走"的判定从各个业务方法里抽出来，集中成一张可展示、可追溯的规则表。
 * 业务方法调用 {@link #assertPass} 拦截，前端调用 {@link #check} 提前展示红黄绿灯。</p>
 */
public interface RiceComplianceGuardService {

    /** 动作：入库收储 */
    String ACTION_STORE = "STORE";
    /** 动作：碾米加工 */
    String ACTION_MILL = "MILL";
    /** 动作：防伪码激活（包装上市） */
    String ACTION_ACTIVATE = "ACTIVATE";
    /** 动作：终端销售/扫码出库 */
    String ACTION_SELL = "SELL";

    /**
     * 执行一组校验并返回全部结果（含通过的，便于前端画清单）
     *
     * @param businessType PLANTING_BATCH/GRAIN_BATCH/STORAGE_RECEIPT/PRODUCT_BATCH/TRACE_CODE
     * @param businessId   业务 ID
     * @param action       STORE/MILL/ACTIVATE/SELL
     */
    List<ComplianceCheckVO> check(String businessType, String businessId, String action);

    /**
     * 校验并阻断：存在任一 BLOCK 级未通过项时抛 42201，消息为全部拦截原因
     */
    void assertPass(String businessType, String businessId, String action);
}
