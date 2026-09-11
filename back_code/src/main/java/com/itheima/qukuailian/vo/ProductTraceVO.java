package com.itheima.qukuailian.vo;

import com.itheima.qukuailian.entity.RiceField;
import com.itheima.qukuailian.entity.RiceMillingBatch;
import com.itheima.qukuailian.entity.RicePlantingBatch;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceStorageReceipt;
import com.itheima.qukuailian.entity.RiceTraceCode;
import lombok.Data;

import java.util.List;

/**
 * 成品批次溯源链路（前端 ProductList "溯源" 弹窗）
 * <p>链路：成品批次 → 加工批次 → 入库单 → 种植批次 → 地块 + 防伪码</p>
 */
@Data
public class ProductTraceVO {

    private RiceProductBatch productBatch;

    private RiceMillingBatch millingBatch;

    private RiceStorageReceipt storageReceipt;

    private RicePlantingBatch plantingBatch;

    private RiceField field;

    /** 该成品批次下的防伪码（默认返回最近 50 个） */
    private List<RiceTraceCode> traceCodes;
}
