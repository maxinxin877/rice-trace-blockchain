package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.service.RiceProductBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiceProductBatchServiceImpl extends ServiceImpl<RiceProductBatchMapper, RiceProductBatch>
        implements RiceProductBatchService {

    @Override
    public RiceProductBatch create(ProductBatchCreateDTO dto) {
        // productBatchId 业务唯一
        long count = lambdaQuery().eq(RiceProductBatch::getProductBatchId, dto.getProductBatchId()).count();
        if (count > 0) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "成品米批次已存在: " + dto.getProductBatchId());
        }
        RiceProductBatch batch = new RiceProductBatch();
        BeanUtils.copyProperties(dto, batch);
        batch.setStatus("DRAFT");
        save(batch);
        log.info("成品米批次创建成功: productBatchId={}, productName={}", batch.getProductBatchId(), dto.getProductName());
        return batch;
    }

    @Override
    public IPage<RiceProductBatch> page(long pageNo, long pageSize, String productBatchId, String productName,
                                        String brandName, String status) {
        LambdaQueryWrapper<RiceProductBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(productBatchId), RiceProductBatch::getProductBatchId, productBatchId)
                .like(StringUtils.hasText(productName), RiceProductBatch::getProductName, productName)
                .eq(StringUtils.hasText(brandName), RiceProductBatch::getBrandName, brandName)
                .eq(StringUtils.hasText(status), RiceProductBatch::getStatus, status)
                .orderByDesc(RiceProductBatch::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }
}
