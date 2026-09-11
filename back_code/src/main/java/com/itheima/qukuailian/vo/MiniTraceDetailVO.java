package com.itheima.qukuailian.vo;

import lombok.Data;

/**
 * 小程序扫码溯源详情（接口文档 v1.1 §6.2）
 */
@Data
public class MiniTraceDetailVO {

    private String traceCode;

    /** 产品信息：产品名称、品牌、规格、执行标准、营养成分 */
    private Object product;

    /** 真伪鉴别摘要（不增加扫码次数） */
    private Object authenticity;

    /** 种植信息：基地照片、品种、种植户、地块位置 */
    private Object planting;

    /** 农事记录时间轴 */
    private Object farmingLogs;

    /** 收储信息：原粮等级、入库时间、仓储环境 */
    private Object storage;

    /** 加工信息：工艺流程、产出率、出厂质检摘要 */
    private Object milling;

    /** 认证证书 */
    private Object certifications;

    /** 链上核验摘要 */
    private Object chainProof;
}
