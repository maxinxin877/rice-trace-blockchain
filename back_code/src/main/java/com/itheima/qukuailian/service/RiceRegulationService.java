package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.RiskWarningHandleDTO;
import com.itheima.qukuailian.dto.YieldBalanceCheckDTO;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceYieldBalance;

import java.util.Map;

/**
 * 监管服务（接口文档 v1.1 §7 监管与审计 + §5.28 看板统计）
 */
public interface RiceRegulationService {

    /** 执行产量平衡校验：异常时生成风险预警并写入审计日志 */
    RiceYieldBalance checkYieldBalance(YieldBalanceCheckDTO dto, String ip);

    /** 查询产量平衡校验结果 */
    IPage<RiceYieldBalance> pageYieldBalanceResults(long pageNo, long pageSize, String checkId,
                                                    String plantingBatchId, String grainBatchId,
                                                    String productBatchId, String result,
                                                    String startTime, String endTime);

    /** 查询风险预警 */
    IPage<RiceRiskWarning> pageRiskWarnings(long pageNo, long pageSize, String warningType,
                                            String riskLevel, String businessId, Boolean handled);

    /** 处理风险预警：写处理结果 + 审计日志 + 处理摘要上链 */
    RiceRiskWarning handleWarning(String warningId, RiskWarningHandleDTO dto, String ip);

    /** 水稻模块总览统计（接口文档 v1.1 §5.28） */
    Map<String, Object> dashboardSummary();
}
