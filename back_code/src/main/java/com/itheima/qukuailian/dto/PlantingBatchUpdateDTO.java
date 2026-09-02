package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新种植批次请求（接口文档 v1.1 §5.9，完整覆盖 + 审计原因）
 */
@Data
public class PlantingBatchUpdateDTO {

    @NotBlank(message = "地块ID不能为空")
    private String fieldId;

    @NotBlank(message = "水稻品种不能为空")
    private String riceVariety;

    @NotBlank(message = "种子来源不能为空")
    private String seedSource;

    private String seedBatchNo;

    private String seedCertificateFileId;

    @NotNull(message = "播种日期不能为空")
    private LocalDate sowingDate;

    private LocalDate expectedHarvestDate;

    private Boolean organicCertified;

    private Boolean greenCertified;

    private List<String> certificationFileIds;

    /** 修改原因（必填，写入审计日志） */
    @NotBlank(message = "修改原因不能为空")
    private String reason;
}
