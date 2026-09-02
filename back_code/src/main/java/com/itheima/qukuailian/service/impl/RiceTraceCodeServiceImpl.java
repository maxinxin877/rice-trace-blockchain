package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.TraceCodeActivateDTO;
import com.itheima.qukuailian.dto.TraceCodeGenerateDTO;
import com.itheima.qukuailian.entity.RiceGenerateTask;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceRiskWarning;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RiceGenerateTaskMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceRiskWarningMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceTraceCodeService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceTraceCodeServiceImpl extends ServiceImpl<RiceTraceCodeMapper, RiceTraceCode>
        implements RiceTraceCodeService {

    /** 窜货/重复扫码类预警类型 */
    private static final List<String> CHANNEL_WARNING_TYPES = List.of("CHANNEL_CONFLICT", "REPEAT_SCAN");

    private final RiceGenerateTaskMapper generateTaskMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final RiceRiskWarningMapper riskWarningMapper;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    @Value("${app.qr-base-url:pages/rice/trace/index?code=}")
    private String qrBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceGenerateTask generate(TraceCodeGenerateDTO dto, String ip) {
        RiceProductBatch productBatch = productBatchMapper.selectById(dto.getProductBatchId());
        if (productBatch == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "成品米批次不存在: " + dto.getProductBatchId());
        }

        // 1. 创建生成任务
        RiceGenerateTask task = new RiceGenerateTask();
        task.setGenerateTaskId(IdGen.generate("TCG"));
        task.setProductBatchId(dto.getProductBatchId());
        task.setQuantity(dto.getQuantity());
        task.setPackageSpec(dto.getPackageSpec());
        task.setExpectedSaleRegion(dto.getExpectedSaleRegion() == null ? "" : dto.getExpectedSaleRegion());
        task.setExpireDays(dto.getExpireDays() == null ? 0 : dto.getExpireDays());
        task.setStatus("PROCESSING");
        generateTaskMapper.insert(task);

        // 2. 批量生成防伪码（默认状态 GENERATED）
        List<RiceTraceCode> codes = buildCodes(dto);
        saveBatch(codes);
        task.setStatus("SUCCESS");
        generateTaskMapper.updateById(task);

        // 3. 生成批次摘要上链
        chainProofService.submit("TRACE_CODE", task.getGenerateTaskId(), digest(task), null);
        auditLogService.record("TRACE_CODE", task.getGenerateTaskId(), "CREATE",
                null, digest(task), "批量生成防伪码", ip);
        log.info("防伪码生成成功: taskId={}, quantity={}", task.getGenerateTaskId(), dto.getQuantity());
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activate(TraceCodeActivateDTO dto, String ip) {
        LocalDateTime activatedAt = dto.getActivatedAt() == null ? LocalDateTime.now() : dto.getActivatedAt();
        int activatedCount = 0;
        int failedCount = 0;

        for (String traceCode : dto.getTraceCodes()) {
            RiceTraceCode code = lambdaQuery().eq(RiceTraceCode::getTraceCode, traceCode).one();
            // 只有 GENERATED 状态的码、且属于指定成品批次才允许激活（接口文档 §5.25）
            boolean valid = code != null
                    && dto.getProductBatchId().equals(code.getProductBatchId())
                    && "GENERATED".equals(code.getStatus());
            if (!valid) {
                failedCount++;
                continue;
            }
            code.setStatus("ACTIVATED");
            code.setActivatedAt(activatedAt);
            code.setChainStatus("PENDING");
            updateById(code);
            activatedCount++;
        }

        if (activatedCount > 0) {
            // 激活状态摘要上链
            Map<String, Object> summary = new HashMap<>();
            summary.put("productBatchId", dto.getProductBatchId());
            summary.put("activatedCount", activatedCount);
            summary.put("activatedBy", dto.getActivatedBy());
            summary.put("activatedAt", activatedAt);
            chainProofService.submit("TRACE_CODE", dto.getProductBatchId(), digest(summary), null);

            // 成品米批次进入 ON_SALE
            RiceProductBatch productBatch = productBatchMapper.selectById(dto.getProductBatchId());
            if (productBatch != null && !"ON_SALE".equals(productBatch.getStatus())) {
                productBatch.setStatus("ON_SALE");
                productBatchMapper.updateById(productBatch);
            }
            auditLogService.record("TRACE_CODE", dto.getProductBatchId(), "UPDATE", null,
                    digest(summary), StringUtils.hasText(dto.getReason()) ? dto.getReason() : "激活防伪码", ip);
        }

        log.info("防伪码激活: productBatch={}, 成功={}, 失败={}", dto.getProductBatchId(), activatedCount, failedCount);
        Map<String, Object> result = new HashMap<>();
        result.put("activatedCount", activatedCount);
        result.put("failedCount", failedCount);
        result.put("chainStatus", "PENDING");
        return result;
    }

    @Override
    public IPage<RiceTraceCode> page(long pageNo, long pageSize, String productBatchId, String traceCode,
                                     String status, String riskLevel) {
        LambdaQueryWrapper<RiceTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(productBatchId), RiceTraceCode::getProductBatchId, productBatchId)
                .eq(StringUtils.hasText(traceCode), RiceTraceCode::getTraceCode, traceCode)
                .eq(StringUtils.hasText(status), RiceTraceCode::getStatus, status)
                .eq(StringUtils.hasText(riskLevel), RiceTraceCode::getRiskLevel, riskLevel)
                .orderByDesc(RiceTraceCode::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public IPage<RiceRiskWarning> pageChannelWarnings(long pageNo, long pageSize, String productBatchId,
                                                      String traceCode, String riskLevel, String region,
                                                      String startTime, String endTime) {
        LambdaQueryWrapper<RiceRiskWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RiceRiskWarning::getWarningType, CHANNEL_WARNING_TYPES)
                .eq(StringUtils.hasText(traceCode), RiceRiskWarning::getBusinessId, traceCode)
                .eq(StringUtils.hasText(riskLevel), RiceRiskWarning::getRiskLevel, riskLevel)
                .ge(StringUtils.hasText(startTime), RiceRiskWarning::getCreateTime, LocalDateTime.parse(startTime))
                .le(StringUtils.hasText(endTime), RiceRiskWarning::getCreateTime, LocalDateTime.parse(endTime))
                .orderByDesc(RiceRiskWarning::getCreateTime);
        return riskWarningMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    /** 构造防伪码列表：RC + 日期 + 8 位序号，二维码内容指向小程序溯源页 */
    private List<RiceTraceCode> buildCodes(TraceCodeGenerateDTO dto) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int offset = ThreadLocalRandom.current().nextInt(100000, 900000);
        List<RiceTraceCode> codes = new ArrayList<>(dto.getQuantity());
        for (int i = 0; i < dto.getQuantity(); i++) {
            String code = "RC" + date + String.format("%08d", offset + i);
            RiceTraceCode entity = new RiceTraceCode();
            entity.setTraceCode(code);
            entity.setQrCodeUrl(qrBaseUrl + code);
            entity.setProductBatchId(dto.getProductBatchId());
            entity.setPackageSpec(dto.getPackageSpec());
            entity.setStatus("GENERATED");
            entity.setExpireDays(dto.getExpireDays() == null ? 0 : dto.getExpireDays());
            entity.setExpectedSaleRegion(dto.getExpectedSaleRegion() == null ? "" : dto.getExpectedSaleRegion());
            entity.setScanCount(0);
            entity.setRiskLevel("LOW");
            entity.setChainStatus("PENDING");
            codes.add(entity);
        }
        return codes;
    }

    private String digest(Object obj) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return HashUtils.sha256(String.valueOf(obj));
        }
    }
}
