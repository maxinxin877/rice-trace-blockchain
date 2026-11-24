package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.DisposalCreateDTO;
import com.itheima.qukuailian.dto.DisposalReviewDTO;
import com.itheima.qukuailian.entity.RiceNonconformingDisposal;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.mapper.RiceNonconformingDisposalMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceStorageReceiptMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceNonconformingDisposalService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import com.itheima.qukuailian.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 不合格品处置实现：
 * <ul>
 *   <li>隔离效果由 {@code isolated=1} + 防错校验共同保证，不覆写批次原始状态</li>
 *   <li>评审与关闭两个关键节点上链，满足"处置结果可核验"</li>
 *   <li>处置方式决定解冻时机：放行类（RELEASE/CONCESSION/REWORK/REPAIR）执行即解冻，
 *       封存类（ISOLATE/SCRAP）保持隔离直到关闭</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiceNonconformingDisposalServiceImpl
        extends ServiceImpl<RiceNonconformingDisposalMapper, RiceNonconformingDisposal>
        implements RiceNonconformingDisposalService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 执行后即可解除隔离的处置方式 */
    private static final List<String> RELEASE_DISPOSITIONS = List.of("RELEASE", "CONCESSION", "REWORK", "REPAIR");

    private final RiceProductBatchMapper productBatchMapper;
    private final RiceStorageReceiptMapper storageReceiptMapper;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    /** 注册 JavaTime 模块，保证处置单摘要哈希基于真实 JSON 而非 toString */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceNonconformingDisposal create(DisposalCreateDTO dto, String ip) {
        RiceNonconformingDisposal disposal = new RiceNonconformingDisposal();
        disposeCopy(dto, disposal);
        disposal.setDisposalId(IdGen.generate("NCD"));
        disposal.setDisposalNo(buildNo());
        disposal.setStatus("DRAFT");
        disposal.setIsolated(false);
        disposal.setApplicant(currentUser());
        disposal.setAppliedAt(LocalDateTime.now());
        disposal.setRiskLevel(StringUtils.hasText(dto.getRiskLevel()) ? dto.getRiskLevel() : "MEDIUM");
        disposal.setQuantity(dto.getQuantity() == null ? 0 : dto.getQuantity());
        disposal.setWeightKg(dto.getWeightKg() == null ? BigDecimal.ZERO : dto.getWeightKg());
        disposal.setChainStatus("PENDING");
        save(disposal);

        auditLogService.record("NONCONFORMING_DISPOSAL", disposal.getDisposalId(), "CREATE", null,
                digest(disposal), "创建不合格品处置单：" + dto.getDefectDesc(), ip);
        log.info("不合格品处置单创建: no={}, business={}/{}", disposal.getDisposalNo(),
                disposal.getBusinessType(), disposal.getBusinessId());
        return disposal;
    }

    private void disposeCopy(DisposalCreateDTO dto, RiceNonconformingDisposal disposal) {
        disposal.setSourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType() : "MANUAL");
        disposal.setSourceId(dto.getSourceId() == null ? "" : dto.getSourceId());
        disposal.setBusinessType(dto.getBusinessType());
        disposal.setBusinessId(dto.getBusinessId());
        disposal.setDefectType(dto.getDefectType());
        disposal.setDefectDesc(dto.getDefectDesc());
        disposal.setDisposition(dto.getDisposition());

        // 批次号/产品名可自动带出，减少手工填写
        String batchNo = dto.getBatchNo();
        String productName = dto.getProductName();
        if ("PRODUCT_BATCH".equals(dto.getBusinessType())) {
            RiceProductBatch product = productBatchMapper.selectById(dto.getBusinessId());
            if (product != null) {
                productName = StringUtils.hasText(productName) ? productName : product.getProductName();
                batchNo = StringUtils.hasText(batchNo) ? batchNo : product.getProductBatchId();
            }
        } else if ("GRAIN_BATCH".equals(dto.getBusinessType())) {
            RiceStorageReceipt receipt = storageReceiptMapper.selectOne(new LambdaQueryWrapper<RiceStorageReceipt>()
                    .eq(RiceStorageReceipt::getGrainBatchId, dto.getBusinessId())
                    .orderByDesc(RiceStorageReceipt::getStorageTime)
                    .last("LIMIT 1"));
            if (receipt != null) {
                batchNo = StringUtils.hasText(batchNo) ? batchNo : receipt.getGrainBatchId();
                productName = StringUtils.hasText(productName) ? productName
                        : "原粮 " + receipt.getGrainGrade();
            }
        }
        disposal.setBatchNo(batchNo == null ? "" : batchNo);
        disposal.setProductName(productName == null ? "" : productName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceNonconformingDisposal submit(String disposalId, String ip) {
        RiceNonconformingDisposal disposal = mustGet(disposalId);
        assertStatus(disposal, "DRAFT", "只有草稿状态可以提交评审");
        disposal.setStatus("PENDING_REVIEW");
        updateById(disposal);
        auditLogService.record("NONCONFORMING_DISPOSAL", disposalId, "UPDATE", null, digest(disposal),
                "提交不合格品评审", ip);
        return disposal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceNonconformingDisposal review(String disposalId, DisposalReviewDTO dto, String ip) {
        RiceNonconformingDisposal disposal = mustGet(disposalId);
        assertStatus(disposal, "PENDING_REVIEW", "只有待评审状态可以审核");

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        disposal.setStatus(approved ? "APPROVED" : "REJECTED");
        disposal.setReviewer(currentUser());
        disposal.setReviewedAt(LocalDateTime.now());
        disposal.setReviewComment(dto.getComment() == null ? "" : dto.getComment());
        // 通过评审即隔离（放行类处置在评审阶段也需要先冻住，执行后才放开）
        disposal.setIsolated(approved);
        updateById(disposal);

        chainProofService.submit("NONCONFORMING_DISPOSAL", disposalId, digest(disposal), null);
        auditLogService.record("NONCONFORMING_DISPOSAL", disposalId, "HANDLE", null, digest(disposal),
                (approved ? "评审通过并隔离：" : "评审驳回：") + disposal.getReviewComment(), ip);
        log.info("不合格品评审: no={}, result={}, isolated={}", disposal.getDisposalNo(),
                disposal.getStatus(), disposal.getIsolated());
        return disposal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceNonconformingDisposal execute(String disposalId, String remark, String ip) {
        RiceNonconformingDisposal disposal = mustGet(disposalId);
        assertStatus(disposal, "APPROVED", "只有评审通过的单据可以执行处置");

        disposal.setStatus("EXECUTED");
        disposal.setExecutedAt(LocalDateTime.now());
        disposal.setExecuteRemark(remark == null ? "" : remark);
        // 放行类处置执行后解除隔离；封存类（隔离/报废）继续冻结直到关闭
        if (RELEASE_DISPOSITIONS.contains(disposal.getDisposition())) {
            disposal.setIsolated(false);
        }
        updateById(disposal);

        chainProofService.submit("NONCONFORMING_DISPOSAL", disposalId, digest(disposal), null);
        auditLogService.record("NONCONFORMING_DISPOSAL", disposalId, "UPDATE", null, digest(disposal),
                "执行处置：" + disposal.getDisposition() + " / " + disposal.getExecuteRemark(), ip);
        return disposal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceNonconformingDisposal close(String disposalId, String remark, String ip) {
        RiceNonconformingDisposal disposal = mustGet(disposalId);
        if (!List.of("APPROVED", "EXECUTED").contains(disposal.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(),
                    "只有评审通过或已执行的单据可以关闭（当前状态：" + disposal.getStatus() + "）");
        }
        disposal.setStatus("CLOSED");
        disposal.setIsolated(false);
        disposal.setExecuteRemark(StringUtils.hasText(remark) ? remark : disposal.getExecuteRemark());
        updateById(disposal);

        chainProofService.submit("NONCONFORMING_DISPOSAL", disposalId, digest(disposal), null);
        auditLogService.record("NONCONFORMING_DISPOSAL", disposalId, "UPDATE", null, digest(disposal),
                "关闭处置单：" + disposal.getExecuteRemark(), ip);
        return disposal;
    }

    @Override
    public IPage<RiceNonconformingDisposal> page(long pageNo, long pageSize, String status, String disposition,
                                                 String businessType, String businessId, String keyword,
                                                 String riskLevel, String startTime, String endTime) {
        LambdaQueryWrapper<RiceNonconformingDisposal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), RiceNonconformingDisposal::getStatus, status)
                .eq(StringUtils.hasText(disposition), RiceNonconformingDisposal::getDisposition, disposition)
                .eq(StringUtils.hasText(businessType), RiceNonconformingDisposal::getBusinessType, businessType)
                .eq(StringUtils.hasText(businessId), RiceNonconformingDisposal::getBusinessId, businessId)
                .eq(StringUtils.hasText(riskLevel), RiceNonconformingDisposal::getRiskLevel, riskLevel)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(RiceNonconformingDisposal::getDisposalNo, keyword)
                        .or().like(RiceNonconformingDisposal::getBatchNo, keyword)
                        .or().like(RiceNonconformingDisposal::getProductName, keyword)
                        .or().like(RiceNonconformingDisposal::getDefectDesc, keyword))
                .ge(StringUtils.hasText(startTime), RiceNonconformingDisposal::getCreateTime, parseTime(startTime))
                .le(StringUtils.hasText(endTime), RiceNonconformingDisposal::getCreateTime, parseTime(endTime))
                .orderByDesc(RiceNonconformingDisposal::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public RiceNonconformingDisposal detail(String disposalId) {
        return mustGet(disposalId);
    }

    // ------------------------------------------------------------------
    private RiceNonconformingDisposal mustGet(String disposalId) {
        RiceNonconformingDisposal disposal = getById(disposalId);
        if (disposal == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "处置单不存在: " + disposalId);
        }
        return disposal;
    }

    private void assertStatus(RiceNonconformingDisposal disposal, String expected, String message) {
        if (!expected.equals(disposal.getStatus())) {
            throw new BizException(ResultCode.CONFLICT.getCode(),
                    message + "（当前状态：" + disposal.getStatus() + "）");
        }
    }

    private String buildNo() {
        String date = LocalDateTime.now().format(DATE_FMT);
        long count = lambdaQuery().likeRight(RiceNonconformingDisposal::getDisposalNo, "NC-" + date).count();
        return "NC-" + date + "-" + String.format("%04d", count + 1);
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return value.length() <= 10
                    ? LocalDate.parse(value).atStartOfDay()
                    : LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String currentUser() {
        try {
            return UserContext.getUsername();
        } catch (Exception e) {
            return "system";
        }
    }

    private String digest(Object obj) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(obj));
        }
    }
}
