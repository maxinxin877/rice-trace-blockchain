package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.dto.PackageUnitCreateDTO;
import com.itheima.qukuailian.entity.RicePackageUnit;
import com.itheima.qukuailian.entity.RicePackageUnitItem;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceTraceCode;
import com.itheima.qukuailian.mapper.RicePackageUnitItemMapper;
import com.itheima.qukuailian.mapper.RicePackageUnitMapper;
import com.itheima.qukuailian.mapper.RiceProductBatchMapper;
import com.itheima.qukuailian.mapper.RiceTraceCodeMapper;
import com.itheima.qukuailian.service.AuditLogService;
import com.itheima.qukuailian.service.RicePackageUnitService;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 箱码/托盘码实现：
 * 关键约束——一个子码只能被一个父码绑定（数据库唯一索引 + 应用层校验双保险），
 * 否则追溯时会一条码挂到多个箱，责任无法定位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RicePackageUnitServiceImpl extends ServiceImpl<RicePackageUnitMapper, RicePackageUnit>
        implements RicePackageUnitService {

    private final RicePackageUnitItemMapper unitItemMapper;
    private final RiceTraceCodeMapper traceCodeMapper;
    private final RiceProductBatchMapper productBatchMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RicePackageUnit create(PackageUnitCreateDTO dto, String ip) {
        String level = StringUtils.hasText(dto.getUnitLevel()) ? dto.getUnitLevel().toUpperCase() : "BOX";
        if (!List.of("BOX", "PALLET").contains(level)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "单元层级只能是 BOX 或 PALLET");
        }
        List<String> childCodes = dto.getChildCodes().stream()
                .filter(StringUtils::hasText).map(String::trim).distinct().toList();
        if (childCodes.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "子码不能为空");
        }

        RicePackageUnit unit = new RicePackageUnit();
        unit.setUnitLevel(level);
        unit.setChildCount(childCodes.size());
        unit.setStatus("SEALED");
        unit.setPackageSpec(dto.getPackageSpec() == null ? "" : dto.getPackageSpec());
        unit.setExpectedSaleRegion(dto.getExpectedSaleRegion() == null ? "" : dto.getExpectedSaleRegion());
        unit.setParentCode("");

        if ("BOX".equals(level)) {
            if (!StringUtils.hasText(dto.getProductBatchId())) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "箱码必须指定成品批次");
            }
            RiceProductBatch product = productBatchMapper.selectById(dto.getProductBatchId());
            if (product == null) {
                throw new BizException(ResultCode.NOT_FOUND.getCode(),
                        "成品批次不存在: " + dto.getProductBatchId());
            }
            if (!StringUtils.hasText(unit.getPackageSpec())) {
                unit.setPackageSpec(product.getPackageSpec());
            }
            if (!StringUtils.hasText(unit.getExpectedSaleRegion())) {
                unit.setExpectedSaleRegion(product.getExpectedSaleRegion());
            }
            unit.setProductBatchId(dto.getProductBatchId());
            unit.setChildLevel("TRACE_CODE");
            unit.setTotalQuantity(childCodes.size());

            // 单品码校验：存在、同批次、未停用、未重复绑定
            for (String code : childCodes) {
                RiceTraceCode traceCode = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                        .eq(RiceTraceCode::getTraceCode, code).last("LIMIT 1"));
                if (traceCode == null) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "单品码不存在: " + code);
                }
                if (!dto.getProductBatchId().equals(traceCode.getProductBatchId())) {
                    throw new BizException(ResultCode.VALIDATION_FAILED.getCode(),
                            "单品码 " + code + " 不属于成品批次 " + dto.getProductBatchId());
                }
                if ("DISABLED".equals(traceCode.getStatus())) {
                    throw new BizException(ResultCode.VALIDATION_FAILED.getCode(),
                            "单品码 " + code + " 已停用（可能处于召回冻结），禁止装箱");
                }
                Long bound = unitItemMapper.selectCount(new LambdaQueryWrapper<RicePackageUnitItem>()
                        .eq(RicePackageUnitItem::getChildCode, code));
                if (bound != null && bound > 0) {
                    throw new BizException(ResultCode.CONFLICT.getCode(),
                            "单品码 " + code + " 已被装箱，禁止重复绑定");
                }
            }
        } else {
            // 托盘：子码必须是已存在的箱码，且同批次
            String productBatchId = dto.getProductBatchId();
            int total = 0;
            for (String code : childCodes) {
                RicePackageUnit box = getById(code);
                if (box == null || !"BOX".equals(box.getUnitLevel())) {
                    throw new BizException(ResultCode.NOT_FOUND.getCode(), "箱码不存在: " + code);
                }
                if (StringUtils.hasText(box.getParentCode())) {
                    throw new BizException(ResultCode.CONFLICT.getCode(),
                            "箱码 " + code + " 已属于托盘 " + box.getParentCode());
                }
                if (!StringUtils.hasText(productBatchId)) {
                    productBatchId = box.getProductBatchId();
                } else if (!productBatchId.equals(box.getProductBatchId())) {
                    throw new BizException(ResultCode.VALIDATION_FAILED.getCode(),
                            "一个托盘只能装同一成品批次的箱，冲突箱码：" + code);
                }
                total += box.getTotalQuantity() == null ? 0 : box.getTotalQuantity();
            }
            unit.setProductBatchId(productBatchId);
            unit.setChildLevel("BOX");
            unit.setTotalQuantity(total);
        }

        unit.setUnitCode(generateUnitCode(level));
        save(unit);

        for (String childCode : childCodes) {
            RicePackageUnitItem item = new RicePackageUnitItem();
            item.setUnitCode(unit.getUnitCode());
            item.setChildCode(childCode);
            item.setChildLevel(unit.getChildLevel());
            unitItemMapper.insert(item);
            if ("PALLET".equals(level)) {
                RicePackageUnit box = getById(childCode);
                box.setParentCode(unit.getUnitCode());
                updateById(box);
            }
        }

        auditLogService.record("PACKAGE_UNIT", unit.getUnitCode(), "CREATE", null, null,
                "创建" + ("BOX".equals(level) ? "箱码" : "托盘码") + "，绑定 " + childCodes.size() + " 个子码", ip);
        log.info("包装单元创建: code={}, level={}, children={}, totalQty={}",
                unit.getUnitCode(), level, childCodes.size(), unit.getTotalQuantity());
        return unit;
    }

    @Override
    public IPage<RicePackageUnit> page(long pageNo, long pageSize, String unitLevel, String productBatchId,
                                       String status, String unitCode) {
        LambdaQueryWrapper<RicePackageUnit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(unitLevel), RicePackageUnit::getUnitLevel, unitLevel)
                .eq(StringUtils.hasText(productBatchId), RicePackageUnit::getProductBatchId, productBatchId)
                .eq(StringUtils.hasText(status), RicePackageUnit::getStatus, status)
                .like(StringUtils.hasText(unitCode), RicePackageUnit::getUnitCode, unitCode)
                .orderByDesc(RicePackageUnit::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public Map<String, Object> detail(String unitCode) {
        RicePackageUnit unit = getById(unitCode);
        if (unit == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "箱码/托盘码不存在: " + unitCode);
        }
        List<RicePackageUnitItem> items = unitItemMapper.selectList(new LambdaQueryWrapper<RicePackageUnitItem>()
                .eq(RicePackageUnitItem::getUnitCode, unitCode)
                .orderByAsc(RicePackageUnitItem::getId));

        List<Map<String, Object>> children = new ArrayList<>();
        for (RicePackageUnitItem item : items) {
            Map<String, Object> child = new LinkedHashMap<>();
            child.put("childCode", item.getChildCode());
            child.put("childLevel", item.getChildLevel());
            if ("BOX".equals(item.getChildLevel())) {
                RicePackageUnit box = getById(item.getChildCode());
                child.put("childCount", box == null ? 0 : box.getChildCount());
                child.put("totalQuantity", box == null ? 0 : box.getTotalQuantity());
                child.put("status", box == null ? "" : box.getStatus());
            } else {
                RiceTraceCode traceCode = traceCodeMapper.selectOne(new LambdaQueryWrapper<RiceTraceCode>()
                        .eq(RiceTraceCode::getTraceCode, item.getChildCode()).last("LIMIT 1"));
                child.put("status", traceCode == null ? "" : traceCode.getStatus());
                child.put("scanCount", traceCode == null ? 0 : traceCode.getScanCount());
                child.put("packageSpec", traceCode == null ? "" : traceCode.getPackageSpec());
            }
            children.add(child);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unit", unit);
        result.put("children", children);
        result.put("traceCodes", expandTraceCodes(unitCode));
        if ("BOX".equals(unit.getUnitLevel())) {
            result.put("parent", StringUtils.hasText(unit.getParentCode()) ? getById(unit.getParentCode()) : null);
        }
        return result;
    }

    @Override
    public List<String> expandTraceCodes(String unitCode) {
        RicePackageUnit unit = getById(unitCode);
        if (unit == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "箱码/托盘码不存在: " + unitCode);
        }
        Set<String> codes = new LinkedHashSet<>();
        if ("BOX".equals(unit.getUnitLevel())) {
            codes.addAll(childCodesOf(unitCode));
        } else {
            for (String boxCode : childCodesOf(unitCode)) {
                codes.addAll(childCodesOf(boxCode));
            }
        }
        return new ArrayList<>(codes);
    }

    @Override
    public Map<String, Object> traceUpward(String traceCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traceCode", traceCode);
        RicePackageUnitItem boxItem = unitItemMapper.selectOne(new LambdaQueryWrapper<RicePackageUnitItem>()
                .eq(RicePackageUnitItem::getChildCode, traceCode)
                .eq(RicePackageUnitItem::getChildLevel, "TRACE_CODE")
                .last("LIMIT 1"));
        if (boxItem == null) {
            result.put("box", null);
            result.put("pallet", null);
            return result;
        }
        RicePackageUnit box = getById(boxItem.getUnitCode());
        result.put("box", box);
        RicePackageUnitItem palletItem = unitItemMapper.selectOne(new LambdaQueryWrapper<RicePackageUnitItem>()
                .eq(RicePackageUnitItem::getChildCode, boxItem.getUnitCode())
                .eq(RicePackageUnitItem::getChildLevel, "BOX")
                .last("LIMIT 1"));
        result.put("pallet", palletItem == null ? null : getById(palletItem.getUnitCode()));
        return result;
    }

    private List<String> childCodesOf(String unitCode) {
        return unitItemMapper.selectList(new LambdaQueryWrapper<RicePackageUnitItem>()
                        .eq(RicePackageUnitItem::getUnitCode, unitCode)
                        .orderByAsc(RicePackageUnitItem::getId))
                .stream().map(RicePackageUnitItem::getChildCode).toList();
    }

    /** 生成不重复的单元码：BOX20261102XXXXXX / PLT20261102XXXXXX */
    private String generateUnitCode(String level) {
        String prefix = "BOX".equals(level) ? "BOX" : "PLT";
        for (int i = 0; i < 5; i++) {
            String code = IdGen.generate(prefix);
            if (getById(code) == null) {
                return code;
            }
        }
        // 极端碰撞时退化为时间戳后缀，保证一定唯一
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
