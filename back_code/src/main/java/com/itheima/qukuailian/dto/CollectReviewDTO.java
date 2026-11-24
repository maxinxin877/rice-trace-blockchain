package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 采集记录审核（需求文档 §九 CJ-07 人工补录审核）
 */
@Data
public class CollectReviewDTO {

    @NotNull(message = "审核结论不能为空")
    private Boolean approved;

    @Size(max = 500, message = "审核意见不超过 500 字")
    private String comment;
}
