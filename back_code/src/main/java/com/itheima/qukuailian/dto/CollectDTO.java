package com.itheima.qukuailian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扫码采集入参（需求文档 §九 CJ-01/02/05/06/07）
 */
@Data
public class CollectDTO {

    @NotBlank(message = "扫码内容不能为空")
    @Size(max = 64, message = "码长度超限")
    private String code;

    /** STORE/MILL/ACTIVATE/OUTBOUND/SHELF/SELL/INSPECT */
    @NotBlank(message = "业务动作不能为空")
    @Pattern(regexp = "STORE|MILL|ACTIVATE|OUTBOUND|SHELF|SELL|INSPECT",
            message = "业务动作只能是 STORE/MILL/ACTIVATE/OUTBOUND/SHELF/SELL/INSPECT")
    private String action;

    /** SCANNER/PDA/MOBILE/WEB */
    private String terminalType;

    /** 终端幂等键；不传时由后端按 码+动作+离线时间 生成 */
    private String idempotencyKey;

    /** 终端本地扫码时间（离线补传时与在线采集区分） */
    private LocalDateTime collectTime;

    /** ONLINE/OFFLINE/MANUAL */
    @Pattern(regexp = "ONLINE|OFFLINE|MANUAL", message = "来源只能是 ONLINE/OFFLINE/MANUAL")
    private String source;

    /** 人工补录原因（source=MANUAL 时必填） */
    @Size(max = 500, message = "补录原因不超过 500 字")
    private String reason;

    private String operatorName;
}
