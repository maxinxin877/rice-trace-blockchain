package com.itheima.qukuailian.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交入库质检请求（接口文档 v1.1 §5.17）
 */
@Data
public class QualityTestSubmitDTO {

    @NotBlank(message = "检测机构不能为空")
    private String testAgency;

    @NotNull(message = "检测时间不能为空")
    private LocalDateTime testTime;

    @NotEmpty(message = "检测项目不能为空")
    @Valid
    private List<QualityTestItemDTO> testItems;

    /** 总体结果：PASS/FAIL/PENDING */
    @NotBlank(message = "总体结果不能为空")
    private String overallResult;

    private String reportFileId;

    private String remark;
}
