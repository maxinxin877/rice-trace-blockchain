package com.itheima.qukuailian.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 离线采集批量补传入参（需求文档 §九 CJ-06）
 */
@Data
public class CollectBatchDTO {

    @NotEmpty(message = "补传数据不能为空")
    @Valid
    private List<CollectDTO> records;
}
