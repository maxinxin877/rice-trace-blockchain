import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { authApi } from "@/api/modules/auth";

/** 角色定义 */
export type UserRole =
  | "RICE_ADMIN"
  | "FARMER"
  | "WAREHOUSE"
  | "FACTORY"
  | "BRAND"
  | "REGULATOR";

/** 角色信息 */
export interface RoleInfo {
  code: UserRole;
  label: string;
  description: string;
}

/** 所有角色 */
export const ROLES: RoleInfo[] = [
  { code: "RICE_ADMIN", label: "水稻管理员", description: "全部水稻模块权限" },
  {
    code: "FARMER",
    label: "种植户",
    description: "地块查看、种植批次查看、农事记录新增和查看",
  },
  {
    code: "WAREHOUSE",
    label: "仓储人员",
    description: "入库单新增和查看、入库质检新增",
  },
  {
    code: "FACTORY",
    label: "加工厂人员",
    description: "加工批次新增、查看、完成加工",
  },
  {
    code: "BRAND",
    label: "品牌运营人员",
    description: "成品批次、防伪码、扫码预警、看板",
  },
  {
    code: "REGULATOR",
    label: "监管人员",
    description: "全量查看、产量校验、风险预警、审计日志、链上核验",
  },
];

/**
 * 后端角色名 → 前端角色名 映射
 */
const ROLE_BACKEND_TO_FRONTEND: Record<string, UserRole> = {
  RICE_ADMIN: "RICE_ADMIN",
  FARMER: "FARMER",
  WAREHOUSE_KEEPER: "WAREHOUSE",
  PROCESSING_FACTORY: "FACTORY",
  BRAND_OPERATOR: "BRAND",
  REGULATOR: "REGULATOR",
};

/** 各角色拥有的权限码 */
const ROLE_PERMISSIONS: Record<UserRole, string[]> = {
  RICE_ADMIN: [
    "RICE_FIELD_CREATE",
    "RICE_FIELD_VIEW",
    "RICE_FIELD_UPDATE",
    "RICE_PLANTING_BATCH_CREATE",
    "RICE_PLANTING_BATCH_VIEW",
    "RICE_PLANTING_BATCH_UPDATE",
    "RICE_FARMING_LOG_CREATE",
    "RICE_FARMING_LOG_VIEW",
    "RICE_ENV_RECORD_CREATE",
    "RICE_ENV_RECORD_VIEW",
    "RICE_STORAGE_CREATE",
    "RICE_STORAGE_VIEW",
    "RICE_STORAGE_QUALITY_CREATE",
    "RICE_MILLING_CREATE",
    "RICE_MILLING_VIEW",
    "RICE_MILLING_UPDATE",
    "RICE_PRODUCT_BATCH_CREATE",
    "RICE_PRODUCT_BATCH_VIEW",
    "RICE_TRACE_CODE_GENERATE",
    "RICE_TRACE_CODE_ACTIVATE",
    "RICE_TRACE_CODE_VIEW",
    "RICE_CHANNEL_WARNING_VIEW",
    "RICE_DASHBOARD_VIEW",
    "RICE_REGULATION_CHECK",
    "RICE_REGULATION_VIEW",
    "RICE_AUDIT_VIEW",
    "RICE_CHAIN_PROOF_VIEW",
    "RICE_CHAIN_PROOF_VERIFY",
  ],
  FARMER: [
    "RICE_FIELD_VIEW",
    "RICE_PLANTING_BATCH_VIEW",
    "RICE_FARMING_LOG_CREATE",
    "RICE_FARMING_LOG_VIEW",
    "RICE_ENV_RECORD_VIEW",
    "RICE_DASHBOARD_VIEW",
  ],
  WAREHOUSE: [
    "RICE_STORAGE_CREATE",
    "RICE_STORAGE_VIEW",
    "RICE_STORAGE_QUALITY_CREATE",
    "RICE_FIELD_VIEW",
    "RICE_PLANTING_BATCH_VIEW",
    "RICE_DASHBOARD_VIEW",
  ],
  FACTORY: [
    "RICE_MILLING_CREATE",
    "RICE_MILLING_VIEW",
    "RICE_MILLING_UPDATE",
    "RICE_STORAGE_VIEW",
    "RICE_PRODUCT_BATCH_VIEW",
    "RICE_DASHBOARD_VIEW",
  ],
  BRAND: [
    "RICE_PRODUCT_BATCH_CREATE",
    "RICE_PRODUCT_BATCH_VIEW",
    "RICE_TRACE_CODE_GENERATE",
    "RICE_TRACE_CODE_ACTIVATE",
    "RICE_TRACE_CODE_VIEW",
    "RICE_CHANNEL_WARNING_VIEW",
    "RICE_DASHBOARD_VIEW",
  ],
  REGULATOR: [
    "RICE_FIELD_VIEW",
    "RICE_PLANTING_BATCH_VIEW",
    "RICE_FARMING_LOG_VIEW",
    "RICE_ENV_RECORD_VIEW",
    "RICE_STORAGE_VIEW",
    "RICE_MILLING_VIEW",
    "RICE_PRODUCT_BATCH_VIEW",
    "RICE_TRACE_CODE_VIEW",
    "RICE_CHANNEL_WARNING_VIEW",
    "RICE_DASHBOARD_VIEW",
    "RICE_REGULATION_CHECK",
    "RICE_REGULATION_VIEW",
    "RICE_AUDIT_VIEW",
    "RICE_CHAIN_PROOF_VIEW",
    "RICE_CHAIN_PROOF_VERIFY",
  ],
};

export const useUserStore = defineStore("user", () => {
  // 从 localStorage 恢复角色
  const savedRole = localStorage.getItem("role") as UserRole | null;
  const initialRole: UserRole | null =
    savedRole && ROLES.some((r) => r.code === savedRole) ? savedRole : null;

  /** 当前 token */
  const token = ref<string>(localStorage.getItem("token") || "");
  /** 用户名称 */
  const userName = ref<string>(localStorage.getItem("userName") || "");
  /** 用户角色 */
  const role = ref<UserRole | null>(initialRole);
  /** 权限列表 */
  const permissions = ref<string[]>(
    token.value && role.value ? ROLE_PERMISSIONS[role.value] || [] : [],
  );

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value);

  /** 当前角色信息 */
  const roleInfo = computed(() =>
    role.value ? ROLES.find((r) => r.code === role.value) : undefined,
  );

  /** 检查是否有指定权限 */
  function hasPermission(code: string): boolean {
    return permissions.value.includes(code);
  }

  /** 后端角色名 → 前端角色名 */
  function mapBackendRole(backendRole: string): UserRole {
    return ROLE_BACKEND_TO_FRONTEND[backendRole] || "RICE_ADMIN";
  }

  /**
   * 登录 — 优先调后端 API，失败则回退 mock
   * 返回 Promise，success 为 true 表示登录成功
   */
  async function login(
    username: string,
    password: string,
  ): Promise<{ success: boolean; message: string }> {
    try {
      const response = await authApi.login({ username, password });
      const data = response.data.data; // axiosResponse -> ApiResponse -> LoginResult

      token.value = data.token;
      userName.value = data.user.nickname || data.user.username;
      const frontendRole = mapBackendRole(data.user.role);
      role.value = frontendRole;
      permissions.value = ROLE_PERMISSIONS[frontendRole] || [];

      localStorage.setItem("token", token.value);
      localStorage.setItem("userName", userName.value);
      localStorage.setItem("role", frontendRole);

      return { success: true, message: "登录成功" };
    } catch (err: unknown) {
      // 请求拦截器已经弹了 ElMessage.error，这里静默返回
      const msg = err instanceof Error ? err.message : "登录失败";
      return { success: false, message: msg };
    }
  }

  /** 登出 — 先清本地状态，再异步通知后端（避免跳转时 token 未清空） */
  async function logout() {
    token.value = "";
    userName.value = "";
    role.value = null;
    permissions.value = [];
    localStorage.removeItem("token");
    localStorage.removeItem("userName");
    localStorage.removeItem("role");
    try {
      await authApi.logout();
    } catch {
      // 后端可能无此接口或网络问题，忽略
    }
  }

  /** 初始化（从 localStorage 恢复登录状态） */
  function initFromStorage() {
    const savedToken = localStorage.getItem("token");
    const savedRole = localStorage.getItem("role") as UserRole | null;
    if (savedToken && savedRole && ROLES.some((r) => r.code === savedRole)) {
      token.value = savedToken;
      userName.value = localStorage.getItem("userName") || "";
      role.value = savedRole;
      permissions.value = ROLE_PERMISSIONS[savedRole] || [];
    }
  }

  return {
    token,
    userName,
    role,
    permissions,
    isLoggedIn,
    roleInfo,
    hasPermission,
    login,
    logout,
    initFromStorage,
  };
});
