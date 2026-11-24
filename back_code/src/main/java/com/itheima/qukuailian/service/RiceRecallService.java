package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.RecallApproveDTO;
import com.itheima.qukuailian.dto.RecallCloseDTO;
import com.itheima.qukuailian.dto.RecallCreateDTO;
import com.itheima.qukuailian.dto.RecallItemHandleDTO;
import com.itheima.qukuailian.dto.RecallReleaseDTO;
import com.itheima.qukuailian.entity.RiceRecallItem;
import com.itheima.qukuailian.entity.RiceRecallTask;
import com.itheima.qukuailian.vo.TraceImpactVO;

/**
 * 召回管理服务（需求文档 §六-4 问题追溯与召回管理、§九 FC-05/FC-06、JK-06）
 * <p>状态机：DRAFT → SUBMITTED → APPROVED → RELEASED → HANDLING → CLOSED；
 * SUBMITTED 可 REJECTED；未发布可 CANCELLED。</p>
 */
public interface RiceRecallService {

    /** 影响范围预览（创建召回前先看会波及多少产品/码/重量/区域） */
    TraceImpactVO previewScope(String businessType, String businessId);

    /** 创建召回任务：自动计算影响范围并生成明细，状态 DRAFT */
    RiceRecallTask create(RecallCreateDTO dto, String ip);

    /** 提交审批：DRAFT → SUBMITTED */
    RiceRecallTask submit(String recallTaskId, String ip);

    /** 审批通过：SUBMITTED → APPROVED */
    RiceRecallTask approve(String recallTaskId, RecallApproveDTO dto, String ip);

    /** 审批驳回：SUBMITTED → REJECTED */
    RiceRecallTask reject(String recallTaskId, RecallApproveDTO dto, String ip);

    /** 发布召回：APPROVED → RELEASED，同时冻结批次/防伪码、下发通知、召回指令上链 */
    RiceRecallTask release(String recallTaskId, RecallReleaseDTO dto, String ip);

    /** 明细处置：更新下架/回收/隔离/销毁等状态，任务自动进入 HANDLING */
    RiceRecallItem handleItem(String itemId, RecallItemHandleDTO dto, String ip);

    /** 关闭召回：RELEASED/HANDLING → CLOSED，处置结果上链 */
    RiceRecallTask close(String recallTaskId, RecallCloseDTO dto, String ip);

    /** 取消召回：未发布状态可取消 */
    RiceRecallTask cancel(String recallTaskId, String reason, String ip);

    /** 分页查询 */
    IPage<RiceRecallTask> page(long pageNo, long pageSize, String status, String riskLevel,
                               String triggerType, String keyword, String startTime, String endTime);

    /** 详情：任务 + 明细 + 处置进度 + 链上存证 */
    RiceRecallTask detail(String recallTaskId);
}
