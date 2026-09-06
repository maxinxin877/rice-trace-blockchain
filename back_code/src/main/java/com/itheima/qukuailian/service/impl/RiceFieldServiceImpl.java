package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.FieldCreateDTO;
import com.itheima.qukuailian.dto.FieldPhotoBindDTO;
import com.itheima.qukuailian.dto.FieldUpdateDTO;
import com.itheima.qukuailian.entity.RiceField;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.mapper.RiceFieldMapper;
import com.itheima.qukuailian.mapper.RicePlantingBatchMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.service.RiceFieldService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceFieldServiceImpl extends ServiceImpl<RiceFieldMapper, RiceField> implements RiceFieldService {

    private static final List<String> IN_PLANTING_STATUS = List.of("DRAFT", "PLANTED", "HARVESTED");

    private final RicePlantingBatchMapper plantingBatchMapper;
    private final ChainProofService chainProofService;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceField create(FieldCreateDTO dto, String ip) {
        // 1. 编号唯一性
        long count = lambdaQuery().eq(RiceField::getFieldCode, dto.getFieldCode()).count();
        if (count > 0) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "地块编号已存在: " + dto.getFieldCode());
        }

        // 2. 组装实体
        RiceField field = new RiceField();
        BeanUtils.copyProperties(dto, field);
        field.setFieldId(IdGen.generate("FIELD"));
        // 3. 坐标哈希 = gisBoundary 的 SHA-256（JSON 规范化序列化）
        field.setCoordinateHash(HashUtils.sha256(toJson(dto.getGisBoundary())));
        field.setChainStatus("PENDING");
        save(field);

        // 4. 异步上链存证 + 审计
        chainProofService.submit("FIELD", field.getFieldId(), field.getCoordinateHash(), null);
        auditLogService.record("FIELD", field.getFieldId(), "CREATE", null,
                field.getCoordinateHash(), "创建地块", ip);
        log.info("地块创建成功: fieldId={}, fieldCode={}", field.getFieldId(), field.getFieldCode());
        return field;
    }

    @Override
    public IPage<RiceField> page(long pageNo, long pageSize, String fieldCode, String fieldName,
                                 String farmerName, String district) {
        LambdaQueryWrapper<RiceField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(fieldCode), RiceField::getFieldCode, fieldCode)
                .like(StringUtils.hasText(fieldName), RiceField::getFieldName, fieldName)
                .like(StringUtils.hasText(farmerName), RiceField::getFarmerName, farmerName)
                .eq(StringUtils.hasText(district), RiceField::getDistrict, district)
                .orderByDesc(RiceField::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public RiceField detail(String fieldId) {
        RiceField field = getById(fieldId);
        if (field == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "地块不存在: " + fieldId);
        }
        // 在种批次摘要（未结束：DRAFT/PLANTED/HARVESTED）
        List<RicePlantingBatch> batches = plantingBatchMapper.selectList(
                new LambdaQueryWrapper<RicePlantingBatch>()
                        .eq(RicePlantingBatch::getFieldId, fieldId)
                        .in(RicePlantingBatch::getStatus, IN_PLANTING_STATUS)
                        .orderByDesc(RicePlantingBatch::getCreateTime));
        field.setPlantingBatches(batches);
        return field;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceField update(String fieldId, FieldUpdateDTO dto, String ip) {
        RiceField field = getById(fieldId);
        if (field == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "地块不存在: " + fieldId);
        }
        String beforeHash = field.getCoordinateHash();
        boolean boundaryChanged = !Objects.equals(field.getGisBoundary(), dto.getGisBoundary());

        // 完整覆盖更新
        BeanUtils.copyProperties(dto, field);
        field.setFieldId(fieldId);
        if (boundaryChanged) {
            // 边界变更：重算坐标哈希并重新上链
            field.setCoordinateHash(HashUtils.sha256(toJson(dto.getGisBoundary())));
            field.setChainStatus("PENDING");
        }
        updateById(field);

        if (boundaryChanged) {
            chainProofService.submit("FIELD", fieldId, field.getCoordinateHash(), null);
        }
        auditLogService.record("FIELD", fieldId, "UPDATE", beforeHash, field.getCoordinateHash(),
                dto.getReason(), ip);
        log.info("地块更新成功: fieldId={}, boundaryChanged={}", fieldId, boundaryChanged);
        return field;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPhotos(String fieldId, FieldPhotoBindDTO dto, String ip) {
        RiceField field = getById(fieldId);
        if (field == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "地块不存在: " + fieldId);
        }
        field.setBasePhotoFileIds(dto.getFileIds());
        updateById(field);
        auditLogService.record("FIELD", fieldId, "UPDATE", null, null, dto.getReason(), ip);
        log.info("地块照片绑定成功: fieldId={}, count={}", fieldId, dto.getFileIds().size());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BizException(ResultCode.ERROR.getCode(), "GIS坐标序列化失败");
        }
    }
}
