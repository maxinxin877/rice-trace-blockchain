package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 召回审批
 */
@Data
public class RecallApproveDTO {

    @Size(max = 500, message = "审批意见不超过 500 字")
    private String comment;
}
