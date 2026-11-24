package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 包装单元（箱/托盘）绑定入参（需求文档 §九 YM-01）
 */
@Data
public class PackageUnitCreateDTO {

    /** BOX：绑定单品码；PALLET：绑定箱码 */
    private String unitLevel;

    /** BOX 必填 */
    private String productBatchId;

    /** 子码清单：BOX 传单品码，PALLET 传箱码 */
    @NotEmpty(message = "子码不能为空")
    private List<String> childCodes;

    private String packageSpec;

    @Size(max = 200, message = "销售区域不超过 200 字")
    private String expectedSaleRegion;
}
