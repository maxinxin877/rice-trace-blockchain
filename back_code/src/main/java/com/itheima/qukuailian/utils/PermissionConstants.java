package com.itheima.qukuailian.utils;

import java.util.Map;
import java.util.Set;

/**
 * 权限码常量与角色-权限映射（接口文档 v1.1 第 10 节权限建议）
 */
public final class PermissionConstants {

    private PermissionConstants() {
    }

    // ---------- 地块档案 ----------
    public static final String RICE_FIELD_CREATE = "RICE_FIELD_CREATE";
    public static final String RICE_FIELD_VIEW = "RICE_FIELD_VIEW";
    public static final String RICE_FIELD_UPDATE = "RICE_FIELD_UPDATE";
    // ---------- 种植批次 ----------
    public static final String RICE_PLANTING_BATCH_CREATE = "RICE_PLANTING_BATCH_CREATE";
    public static final String RICE_PLANTING_BATCH_VIEW = "RICE_PLANTING_BATCH_VIEW";
    public static final String RICE_PLANTING_BATCH_UPDATE = "RICE_PLANTING_BATCH_UPDATE";
    // ---------- 农事记录 ----------
    public static final String RICE_FARMING_LOG_CREATE = "RICE_FARMING_LOG_CREATE";
    public static final String RICE_FARMING_LOG_VIEW = "RICE_FARMING_LOG_VIEW";
    // ---------- 环境数据 ----------
    public static final String RICE_ENV_RECORD_CREATE = "RICE_ENV_RECORD_CREATE";
    public static final String RICE_ENV_RECORD_VIEW = "RICE_ENV_RECORD_VIEW";
    // ---------- 收储入库 ----------
    public static final String RICE_STORAGE_CREATE = "RICE_STORAGE_CREATE";
    public static final String RICE_STORAGE_VIEW = "RICE_STORAGE_VIEW";
    public static final String RICE_STORAGE_QUALITY_CREATE = "RICE_STORAGE_QUALITY_CREATE";
    // ---------- 碾米加工 ----------
    public static final String RICE_MILLING_CREATE = "RICE_MILLING_CREATE";
    public static final String RICE_MILLING_VIEW = "RICE_MILLING_VIEW";
    public static final String RICE_MILLING_UPDATE = "RICE_MILLING_UPDATE";
    // ---------- 成品批次 ----------
    public static final String RICE_PRODUCT_BATCH_CREATE = "RICE_PRODUCT_BATCH_CREATE";
    public static final String RICE_PRODUCT_BATCH_VIEW = "RICE_PRODUCT_BATCH_VIEW";
    // ---------- 品牌防伪 ----------
    public static final String RICE_TRACE_CODE_GENERATE = "RICE_TRACE_CODE_GENERATE";
    public static final String RICE_TRACE_CODE_ACTIVATE = "RICE_TRACE_CODE_ACTIVATE";
    public static final String RICE_TRACE_CODE_VIEW = "RICE_TRACE_CODE_VIEW";
    public static final String RICE_CHANNEL_WARNING_VIEW = "RICE_CHANNEL_WARNING_VIEW";
    public static final String RICE_DASHBOARD_VIEW = "RICE_DASHBOARD_VIEW";
    // ---------- 监管审计 ----------
    public static final String RICE_REGULATION_CHECK = "RICE_REGULATION_CHECK";
    public static final String RICE_REGULATION_VIEW = "RICE_REGULATION_VIEW";
    public static final String RICE_AUDIT_VIEW = "RICE_AUDIT_VIEW";
    public static final String RICE_CHAIN_PROOF_VIEW = "RICE_CHAIN_PROOF_VIEW";
    public static final String RICE_CHAIN_PROOF_VERIFY = "RICE_CHAIN_PROOF_VERIFY";

    /** 角色 -> 权限码集合 */
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.ofEntries(
            Map.entry("RICE_ADMIN", Set.of(
                    RICE_FIELD_CREATE, RICE_FIELD_VIEW, RICE_FIELD_UPDATE,
                    RICE_PLANTING_BATCH_CREATE, RICE_PLANTING_BATCH_VIEW, RICE_PLANTING_BATCH_UPDATE,
                    RICE_FARMING_LOG_CREATE, RICE_FARMING_LOG_VIEW,
                    RICE_ENV_RECORD_CREATE, RICE_ENV_RECORD_VIEW,
                    RICE_STORAGE_CREATE, RICE_STORAGE_VIEW, RICE_STORAGE_QUALITY_CREATE,
                    RICE_MILLING_CREATE, RICE_MILLING_VIEW, RICE_MILLING_UPDATE,
                    RICE_PRODUCT_BATCH_CREATE, RICE_PRODUCT_BATCH_VIEW,
                    RICE_TRACE_CODE_GENERATE, RICE_TRACE_CODE_ACTIVATE, RICE_TRACE_CODE_VIEW,
                    RICE_CHANNEL_WARNING_VIEW, RICE_DASHBOARD_VIEW,
                    RICE_REGULATION_CHECK, RICE_REGULATION_VIEW, RICE_AUDIT_VIEW,
                    RICE_CHAIN_PROOF_VIEW, RICE_CHAIN_PROOF_VERIFY)),
            Map.entry("FARMER", Set.of(
                    RICE_FIELD_VIEW,
                    RICE_PLANTING_BATCH_CREATE, RICE_PLANTING_BATCH_VIEW, RICE_PLANTING_BATCH_UPDATE,
                    RICE_FARMING_LOG_CREATE, RICE_FARMING_LOG_VIEW,
                    RICE_ENV_RECORD_CREATE, RICE_ENV_RECORD_VIEW,
                    RICE_CHAIN_PROOF_VIEW)),
            Map.entry("WAREHOUSE_KEEPER", Set.of(
                    RICE_STORAGE_CREATE, RICE_STORAGE_VIEW, RICE_STORAGE_QUALITY_CREATE,
                    RICE_PLANTING_BATCH_VIEW, RICE_CHAIN_PROOF_VIEW)),
            Map.entry("PROCESSING_FACTORY", Set.of(
                    RICE_MILLING_CREATE, RICE_MILLING_VIEW, RICE_MILLING_UPDATE,
                    RICE_PRODUCT_BATCH_VIEW,
                    RICE_STORAGE_VIEW, RICE_CHAIN_PROOF_VIEW)),
            Map.entry("BRAND_OPERATOR", Set.of(
                    RICE_PRODUCT_BATCH_CREATE, RICE_PRODUCT_BATCH_VIEW,
                    RICE_TRACE_CODE_GENERATE, RICE_TRACE_CODE_ACTIVATE, RICE_TRACE_CODE_VIEW,
                    RICE_CHANNEL_WARNING_VIEW, RICE_DASHBOARD_VIEW, RICE_CHAIN_PROOF_VIEW)),
            Map.entry("REGULATOR", Set.of(
                    RICE_FIELD_VIEW, RICE_PLANTING_BATCH_VIEW, RICE_FARMING_LOG_VIEW, RICE_ENV_RECORD_VIEW,
                    RICE_STORAGE_VIEW, RICE_MILLING_VIEW, RICE_PRODUCT_BATCH_VIEW,
                    RICE_TRACE_CODE_VIEW, RICE_CHANNEL_WARNING_VIEW, RICE_DASHBOARD_VIEW,
                    RICE_REGULATION_CHECK, RICE_REGULATION_VIEW, RICE_AUDIT_VIEW,
                    RICE_CHAIN_PROOF_VIEW, RICE_CHAIN_PROOF_VERIFY))
    );

    /**
     * 获取角色拥有的权限码集合（未知角色返回空集合）
     */
    public static Set<String> permissionsOf(String role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of());
    }
}
