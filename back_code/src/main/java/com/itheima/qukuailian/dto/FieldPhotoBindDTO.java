package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 绑定基地实景照片请求（接口文档 v1.1 §5.5）
 */
@Data
public class FieldPhotoBindDTO {

    @NotEmpty(message = "文件ID列表不能为空")
    private List<String> fileIds;

    @NotBlank(message = "修改原因不能为空")
    private String reason;
}
