package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.PackageUnitCreateDTO;
import com.itheima.qukuailian.entity.RicePackageUnit;

import java.util.List;
import java.util.Map;

/**
 * 包装单元（箱码 / 托盘码）服务（需求文档 §九 YM-01 统一编码体系）
 * <p>编码层级：托盘码 → 箱码 → 单品码 → 批次 → 地块。
 * 扫码时无论扫到哪一层，都能展开到单品与上游批次。</p>
 */
public interface RicePackageUnitService {

    /** 创建箱码或托盘码并绑定子码 */
    RicePackageUnit create(PackageUnitCreateDTO dto, String ip);

    /** 分页查询 */
    IPage<RicePackageUnit> page(long pageNo, long pageSize, String unitLevel, String productBatchId,
                                String status, String unitCode);

    /** 详情：单元信息 + 子码清单（托盘会带出箱及其单品数） */
    Map<String, Object> detail(String unitCode);

    /** 把任意层级展开为单品码清单（托盘→箱→单品） */
    List<String> expandTraceCodes(String unitCode);

    /** 查询某个单品码所属的箱码与托盘码 */
    Map<String, Object> traceUpward(String traceCode);
}
