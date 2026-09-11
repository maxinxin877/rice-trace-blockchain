package com.itheima.qukuailian.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 小程序真伪鉴别请求（接口文档 v1.1 §6.3，支持 JSON 体或查询参数）
 */
@Data
public class MiniVerifyDTO {

    /** 扫码经度 */
    private BigDecimal lng;

    /** 扫码纬度 */
    private BigDecimal lat;

    /** 扫码地区 */
    private String region;

    /** 设备指纹 */
    private String deviceId;

    /** 扫码场景，如 CONSUMER_SCAN */
    private String scene;
}
