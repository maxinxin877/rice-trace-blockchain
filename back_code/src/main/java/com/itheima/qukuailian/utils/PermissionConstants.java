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
    // ---------- 追溯查询（需求 §六-3、§九 ZS-01/ZS-02） ----------
    public static final String RICE_TRACE_QUERY_VIEW = "RICE_TRACE_QUERY_VIEW";
    // ---------- 召回管理（需求 §六-4、§九 FC-05/FC-06） ----------
    public static final String RICE_RECALL_VIEW = "RICE_RECALL_VIEW";
    public static final String RICE_RECALL_CREATE = "RICE_RECALL_CREATE";
    public static final String RICE_RECALL_APPROVE = "RICE_RECALL_APPROVE";
    public static final String RICE_RECALL_HANDLE = "RICE_RECALL_HANDLE";
    // ---------- 生产防错校验（需求 §8.4、§九 FC-01~FC-05） ----------
    public static final String RICE_COMPLIANCE_VIEW = "RICE_COMPLIANCE_VIEW";
    // ---------- 扫码采集（需求 §九 CJ-01~CJ-07） ----------
    public static final String RICE_COLLECT_VIEW = "RICE_COLLECT_VIEW";
    public static final String RICE_COLLECT_CREATE = "RICE_COLLECT_CREATE";
    public static final String RICE_COLLECT_REVIEW = "RICE_COLLECT_REVIEW";
    // ---------- 不合格品处置（需求 §九 FC-04） ----------
    public static final String RICE_DISPOSAL_VIEW = "RICE_DISPOSAL_VIEW";
    public static final String RICE_DISPOSAL_CREATE = "RICE_DISPOSAL_CREATE";
    public static final String RICE_DISPOSAL_REVIEW = "RICE_DISPOSAL_REVIEW";
    // ---------- 箱码/托盘码（需求 §九 YM-01） ----------
    public static final String RICE_PACKAGE_VIEW = "RICE_PACKAGE_VIEW";
    public static final String RICE_PACKAGE_CREATE = "RICE_PACKAGE_CREATE";

    /**
     * 角色 -> 权限码集合
     * <p><b>维护约定：本表必须与前端 {@code front_code/src/stores/user.ts} 的 ROLE_PERMISSIONS
     * 以及菜单定义（router/index.ts、layouts/MainLayout.vue）保持一致。</b>
     * 修改后请执行 {@code back_code/scripts/check-permission-parity.ps1} 校验，避免出现
     * "菜单可见但接口 40301" 或"接口可写但界面无入口"的不一致。</p>
     */
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
                    RICE_CHAIN_PROOF_VIEW, RICE_CHAIN_PROOF_VERIFY,
                    RICE_TRACE_QUERY_VIEW, RICE_COMPLIANCE_VIEW,
                    RICE_RECALL_VIEW, RICE_RECALL_CREATE, RICE_RECALL_APPROVE, RICE_RECALL_HANDLE,
                    RICE_COLLECT_VIEW, RICE_COLLECT_CREATE, RICE_COLLECT_REVIEW,
                    RICE_DISPOSAL_VIEW, RICE_DISPOSAL_CREATE, RICE_DISPOSAL_REVIEW,
                    RICE_PACKAGE_VIEW, RICE_PACKAGE_CREATE)),
            Map.entry("FARMER", Set.of(
                    RICE_FIELD_VIEW,
                    RICE_PLANTING_BATCH_CREATE, RICE_PLANTING_BATCH_VIEW, RICE_PLANTING_BATCH_UPDATE,
                    RICE_FARMING_LOG_CREATE, RICE_FARMING_LOG_VIEW,
                    RICE_ENV_RECORD_CREATE, RICE_ENV_RECORD_VIEW,
                    RICE_DASHBOARD_VIEW,
                    RICE_TRACE_QUERY_VIEW,                     RICE_COMPLIANCE_VIEW,
                    RICE_COLLECT_VIEW, RICE_COLLECT_CREATE)),
            Map.entry("WAREHOUSE_KEEPER", Set.of(
                    RICE_STORAGE_CREATE, RICE_STORAGE_VIEW, RICE_STORAGE_QUALITY_CREATE,
                    RICE_FIELD_VIEW, RICE_PLANTING_BATCH_VIEW,
                    RICE_DASHBOARD_VIEW,
                    RICE_TRACE_QUERY_VIEW, RICE_COMPLIANCE_VIEW,                     RICE_RECALL_VIEW,
                    RICE_COLLECT_VIEW, RICE_COLLECT_CREATE,
                    RICE_DISPOSAL_VIEW, RICE_DISPOSAL_CREATE,
                    RICE_PACKAGE_VIEW)),
            Map.entry("PROCESSING_FACTORY", Set.of(
                    RICE_MILLING_CREATE, RICE_MILLING_VIEW, RICE_MILLING_UPDATE,
                    RICE_PRODUCT_BATCH_VIEW,
                    RICE_STORAGE_VIEW,
                    RICE_DASHBOARD_VIEW,
                    RICE_TRACE_QUERY_VIEW, RICE_COMPLIANCE_VIEW,                     RICE_RECALL_VIEW,
                    RICE_COLLECT_VIEW, RICE_COLLECT_CREATE,
                    RICE_DISPOSAL_VIEW, RICE_DISPOSAL_CREATE,
                    RICE_PACKAGE_VIEW, RICE_PACKAGE_CREATE)),
            Map.entry("BRAND_OPERATOR", Set.of(
                    RICE_PRODUCT_BATCH_CREATE, RICE_PRODUCT_BATCH_VIEW,
                    RICE_TRACE_CODE_GENERATE, RICE_TRACE_CODE_ACTIVATE, RICE_TRACE_CODE_VIEW,
                    RICE_CHANNEL_WARNING_VIEW, RICE_DASHBOARD_VIEW,
                    RICE_TRACE_QUERY_VIEW, RICE_COMPLIANCE_VIEW,
                    RICE_RECALL_VIEW, RICE_RECALL_CREATE, RICE_RECALL_HANDLE,
                    RICE_COLLECT_VIEW, RICE_COLLECT_CREATE,
                    RICE_DISPOSAL_VIEW,
                    RICE_PACKAGE_VIEW, RICE_PACKAGE_CREATE)),
            Map.entry("REGULATOR", Set.of(
                    RICE_FIELD_VIEW, RICE_PLANTING_BATCH_VIEW, RICE_FARMING_LOG_VIEW, RICE_ENV_RECORD_VIEW,
                    RICE_STORAGE_VIEW, RICE_MILLING_VIEW, RICE_PRODUCT_BATCH_VIEW,
                    RICE_TRACE_CODE_VIEW, RICE_CHANNEL_WARNING_VIEW, RICE_DASHBOARD_VIEW,
                    RICE_REGULATION_CHECK, RICE_REGULATION_VIEW, RICE_AUDIT_VIEW,
                    RICE_CHAIN_PROOF_VIEW, RICE_CHAIN_PROOF_VERIFY,
                    RICE_TRACE_QUERY_VIEW, RICE_COMPLIANCE_VIEW,
                    RICE_RECALL_VIEW, RICE_RECALL_CREATE, RICE_RECALL_APPROVE, RICE_RECALL_HANDLE,
                    RICE_COLLECT_VIEW, RICE_COLLECT_REVIEW,
                    RICE_DISPOSAL_VIEW, RICE_DISPOSAL_REVIEW,
                    RICE_PACKAGE_VIEW))
    );

    /**
     * 获取角色拥有的权限码集合（未知角色返回空集合）
     */
    public static Set<String> permissionsOf(String role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of());
    }
}
