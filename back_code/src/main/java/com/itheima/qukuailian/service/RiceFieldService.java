package com.itheima.qukuailian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.qukuailian.dto.FieldCreateDTO;
import com.itheima.qukuailian.dto.FieldPhotoBindDTO;
import com.itheima.qukuailian.dto.FieldUpdateDTO;
import com.itheima.qukuailian.entity.RiceField;

/**
 * 地块档案服务（接口文档 v1.1 §5.1-§5.5）
 */
public interface RiceFieldService {

    /** 创建地块：校验编号唯一、GIS≥3点，计算坐标哈希并异步上链 */
    RiceField create(FieldCreateDTO dto, String ip);

    /** 分页查询（支持 chainStatus 链上状态筛选） */
    IPage<RiceField> page(long pageNo, long pageSize, String fieldCode, String fieldName,
                          String farmerName, String district, String chainStatus);

    /** 详情：含基地照片、坐标哈希、链上交易 ID 与在种批次摘要 */
    RiceField detail(String fieldId);

    /** 更新地块：完整覆盖 + 审计原因；边界变更时重算哈希并上链 */
    RiceField update(String fieldId, FieldUpdateDTO dto, String ip);

    /** 绑定基地实景照片 */
    void bindPhotos(String fieldId, FieldPhotoBindDTO dto, String ip);

    /** 删除地块（存在种植批次时拒绝删除） */
    void delete(String fieldId, String ip);
}
