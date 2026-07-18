import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/** 角色定义 */
export type UserRole = 'RICE_ADMIN' | 'FARMER' | 'WAREHOUSE' | 'FACTORY' | 'BRAND' | 'REGULATOR'

/** 角色信息 */
export interface RoleInfo {
  code: UserRole
  label: string
  description: string
}

/** Mock 账号 */
export interface MockAccount {
  username: string
  password: string
  role: UserRole
  userName: string
}

/** 所有角色 */
export const ROLES: RoleInfo[] = [
  { code: 'RICE_ADMIN', label: '水稻管理员', description: '全部水稻模块权限' },
  { code: 'FARMER', label: '种植户', description: '地块查看、种植批次查看、农事记录新增和查看' },
  { code: 'WAREHOUSE', label: '仓储人员', description: '入库单新增和查看、入库质检新增' },
  { code: 'FACTORY', label: '加工厂人员', description: '加工批次新增、查看、完成加工' },
  { code: 'BRAND', label: '品牌运营人员', description: '成品批次、防伪码、扫码预警、看板' },
  { code: 'REGULATOR', label: '监管人员', description: '全量查看、产量校验、风险预警、审计日志、链上核验' },
]

/** Mock 账号列表 */
export const MOCK_ACCOUNTS: MockAccount[] = [
  { username: 'admin1', password: '123123123', role: 'RICE_ADMIN', userName: '水稻管理员' },
  { username: 'admin2', password: '123123123', role: 'FARMER', userName: '种植户张三丰' },
  { username: 'admin3', password: '123123123', role: 'WAREHOUSE', userName: '仓储员李仓库' },
  { username: 'admin4', password: '123123123', role: 'FACTORY', userName: '加工员王工厂' },
  { username: 'admin5', password: '123123123', role: 'BRAND', userName: '运营员赵品牌' },
  { username: 'admin6', password: '123123123', role: 'REGULATOR', userName: '监管员陈监管' },
]

/**
 * 各角色拥有的权限码
 */
const ROLE_PERMISSIONS: Record<UserRole, string[]> = {
  RICE_ADMIN: [
    // 全部权限
    'RICE_FIELD_CREATE', 'RICE_FIELD_VIEW', 'RICE_FIELD_UPDATE',
    'RICE_PLANTING_BATCH_CREATE', 'RICE_PLANTING_BATCH_VIEW', 'RICE_PLANTING_BATCH_UPDATE',
    'RICE_FARMING_LOG_CREATE', 'RICE_FARMING_LOG_VIEW',
    'RICE_ENV_RECORD_CREATE', 'RICE_ENV_RECORD_VIEW',
    'RICE_STORAGE_CREATE', 'RICE_STORAGE_VIEW', 'RICE_STORAGE_QUALITY_CREATE',
    'RICE_MILLING_CREATE', 'RICE_MILLING_VIEW', 'RICE_MILLING_UPDATE',
    'RICE_PRODUCT_BATCH_CREATE', 'RICE_PRODUCT_BATCH_VIEW',
    'RICE_TRACE_CODE_GENERATE', 'RICE_TRACE_CODE_ACTIVATE', 'RICE_TRACE_CODE_VIEW',
    'RICE_CHANNEL_WARNING_VIEW', 'RICE_DASHBOARD_VIEW',
    'RICE_REGULATION_CHECK', 'RICE_REGULATION_VIEW',
    'RICE_AUDIT_VIEW', 'RICE_CHAIN_PROOF_VIEW', 'RICE_CHAIN_PROOF_VERIFY',
  ],
  FARMER: [
    'RICE_FIELD_VIEW',
    'RICE_PLANTING_BATCH_VIEW',
    'RICE_FARMING_LOG_CREATE', 'RICE_FARMING_LOG_VIEW',
    'RICE_ENV_RECORD_VIEW',
    'RICE_DASHBOARD_VIEW',
  ],
  WAREHOUSE: [
    'RICE_STORAGE_CREATE', 'RICE_STORAGE_VIEW', 'RICE_STORAGE_QUALITY_CREATE',
    'RICE_FIELD_VIEW',
    'RICE_PLANTING_BATCH_VIEW',
    'RICE_DASHBOARD_VIEW',
  ],
  FACTORY: [
    'RICE_MILLING_CREATE', 'RICE_MILLING_VIEW', 'RICE_MILLING_UPDATE',
    'RICE_STORAGE_VIEW',
    'RICE_PRODUCT_BATCH_VIEW',
    'RICE_DASHBOARD_VIEW',
  ],
  BRAND: [
    'RICE_PRODUCT_BATCH_CREATE', 'RICE_PRODUCT_BATCH_VIEW',
    'RICE_TRACE_CODE_GENERATE', 'RICE_TRACE_CODE_ACTIVATE', 'RICE_TRACE_CODE_VIEW',
    'RICE_CHANNEL_WARNING_VIEW',
    'RICE_DASHBOARD_VIEW',
  ],
  REGULATOR: [
    // 全量查看权限
    'RICE_FIELD_VIEW',
    'RICE_PLANTING_BATCH_VIEW',
    'RICE_FARMING_LOG_VIEW',
    'RICE_ENV_RECORD_VIEW',
    'RICE_STORAGE_VIEW',
    'RICE_MILLING_VIEW',
    'RICE_PRODUCT_BATCH_VIEW',
    'RICE_TRACE_CODE_VIEW',
    'RICE_CHANNEL_WARNING_VIEW',
    'RICE_DASHBOARD_VIEW',
    'RICE_REGULATION_CHECK', 'RICE_REGULATION_VIEW',
    'RICE_AUDIT_VIEW',
    'RICE_CHAIN_PROOF_VIEW', 'RICE_CHAIN_PROOF_VERIFY',
  ],
}

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 恢复角色
  const savedRole = localStorage.getItem('role') as UserRole | null
  const initialRole: UserRole = savedRole && ROLES.some((r) => r.code === savedRole) ? savedRole : 'RICE_ADMIN'

  /** 当前 token */
  const token = ref<string>(localStorage.getItem('token') || '')
  /** 用户名称 */
  const userName = ref<string>(localStorage.getItem('userName') || '')
  /** 用户角色 */
  const role = ref<UserRole>(initialRole)
  /** 权限列表 — 根据 localStorage 中的角色初始化 */
  const permissions = ref<string[]>(token.value ? (ROLE_PERMISSIONS[initialRole] || []) : [])

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 当前角色信息 */
  const roleInfo = computed(() => ROLES.find((r) => r.code === role.value))

  /** 检查是否有指定权限 */
  function hasPermission(code: string): boolean {
    return permissions.value.includes(code)
  }

  /** Mock 登录 */
  function login(username: string, password: string): { success: boolean; message: string } {
    const account = MOCK_ACCOUNTS.find(
      (a) => a.username === username && a.password === password
    )
    if (!account) {
      return { success: false, message: '账号或密码错误' }
    }

    token.value = `mock_token_${account.username}_${Date.now()}`
    userName.value = account.userName
    role.value = account.role
    permissions.value = ROLE_PERMISSIONS[account.role] || []

    // 持久化到 localStorage
    localStorage.setItem('token', token.value)
    localStorage.setItem('userName', userName.value)
    localStorage.setItem('role', role.value)

    return { success: true, message: '登录成功' }
  }

  /** 登出 */
  function logout() {
    token.value = ''
    userName.value = ''
    role.value = 'RICE_ADMIN'
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userName')
    localStorage.removeItem('role')
  }

  /** 初始化（从 localStorage 恢复登录状态） */
  function initFromStorage() {
    const savedToken = localStorage.getItem('token')
    const savedRole = localStorage.getItem('role') as UserRole | null
    if (savedToken && savedRole) {
      token.value = savedToken
      userName.value = localStorage.getItem('userName') || ''
      role.value = savedRole
      permissions.value = ROLE_PERMISSIONS[savedRole] || []
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
  }
})
