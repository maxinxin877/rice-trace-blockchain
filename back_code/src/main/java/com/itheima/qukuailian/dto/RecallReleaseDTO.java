package com.itheima.qukuailian.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布召回（冻结生效 + 通知下发）
 */
@Data
public class RecallReleaseDTO {

    /** 通知渠道：SMS/WECHAT/STORE_APP */
    private List<String> notifyChannels;

    /** 处置截止时间（可覆盖创建时的值） */
    private LocalDateTime deadline;
}
