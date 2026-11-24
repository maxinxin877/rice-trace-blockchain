package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 不合格品处置审核（需求文档 §九 FC-04：评审结论决定是否继续冻结）
 */
@Data
public class DisposalReviewDTO {

    @NotNull(message = "审核结论不能为空")
    private Boolean approved;

    @Size(max = 500, message = "审核意见不超过 500 字")
    private String comment;
}
