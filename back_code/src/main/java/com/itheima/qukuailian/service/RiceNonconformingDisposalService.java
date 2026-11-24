package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.DisposalCreateDTO;
import com.itheima.qukuailian.dto.DisposalReviewDTO;
import com.itheima.qukuailian.entity.RiceNonconformingDisposal;

/**
 * 不合格品处置服务（需求文档 §九 FC-04）
 * <p>状态机：DRAFT → PENDING_REVIEW → APPROVED → EXECUTED → CLOSED，PENDING_REVIEW 可 REJECTED。
 * 隔离通过"处置单 isolated 标记 + 防错校验"实现，不直接改批次状态，避免解冻时状态无法还原。</p>
 */
public interface RiceNonconformingDisposalService {

    /** 创建处置单（DRAFT） */
    RiceNonconformingDisposal create(DisposalCreateDTO dto, String ip);

    /** 提交评审 */
    RiceNonconformingDisposal submit(String disposalId, String ip);

    /** 评审：通过则进入隔离，驳回则解除 */
    RiceNonconformingDisposal review(String disposalId, DisposalReviewDTO dto, String ip);

    /** 执行处置（返工/报废/让步接收等），并按结论决定是否解除隔离 */
    RiceNonconformingDisposal execute(String disposalId, String remark, String ip);

    /** 关闭处置单（归档，解除隔离） */
    RiceNonconformingDisposal close(String disposalId, String remark, String ip);

    /** 分页查询 */
    IPage<RiceNonconformingDisposal> page(long pageNo, long pageSize, String status, String disposition,
                                          String businessType, String businessId, String keyword,
                                          String riskLevel, String startTime, String endTime);

    /** 详情 */
    RiceNonconformingDisposal detail(String disposalId);
}
