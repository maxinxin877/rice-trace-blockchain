package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 召回明细处置（下架 / 回收 / 隔离 / 退货 / 销毁 / 未召回）
 */
@Data
public class RecallItemHandleDTO {

    @NotBlank(message = "处置状态不能为空")
    @Pattern(regexp = "PENDING|OFF_SHELF|RECOVERED|ISOLATED|RETURNED|SCRAPPED|UNRECOVERED",
            message = "非法的处置状态")
    private String handleStatus;

    @Size(max = 500, message = "处置说明不超过 500 字")
    private String remark;
}
