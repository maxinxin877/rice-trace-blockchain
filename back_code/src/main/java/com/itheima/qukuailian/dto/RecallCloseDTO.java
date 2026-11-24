package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 召回关闭（需给出处置结论，形成责任闭环）
 */
@Data
public class RecallCloseDTO {

    @NotBlank(message = "关闭结论不能为空")
    @Size(max = 500, message = "关闭结论不超过 500 字")
    private String closeResult;
}
