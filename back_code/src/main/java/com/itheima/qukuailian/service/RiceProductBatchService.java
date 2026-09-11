package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.ProductBatchCreateDTO;
import com.itheima.qukuailian.entity.RiceProductBatch;
import com.itheima.qukuailian.entity.RiceScanLog;
import com.itheima.qukuailian.vo.ProductTraceVO;

/**
 * 成品米批次服务（接口文档 v1.1 §5.22-§5.23）
 */
public interface RiceProductBatchService {

    /** 创建成品米批次：productBatchId 业务唯一 */
    RiceProductBatch create(ProductBatchCreateDTO dto);

    /** 分页查询 */
    IPage<RiceProductBatch> page(long pageNo, long pageSize, String productBatchId, String productName,
                                 String brandName, String status, String riceVariety);

    /** 详情 */
    RiceProductBatch detail(String productBatchId);

    /** 更新成品批次（完整覆盖 + 审计原因） */
    RiceProductBatch update(String productBatchId, ProductBatchCreateDTO dto, String reason, String ip);

    /** 成品批次溯源链路：成品 → 加工 → 入库 → 种植 → 地块 + 防伪码 */
    ProductTraceVO trace(String productBatchId);

    /** 防伪码扫码记录（防伪码列表"扫码记录"弹窗） */
    IPage<RiceScanLog> pageScanLogs(String traceCode, long pageNo, long pageSize);
}
