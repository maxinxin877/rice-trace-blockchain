import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { UserRole } from '@/stores/user'
import MainLayout from '@/layouts/MainLayout.vue'

/** 路由元信息扩展 */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    hidden?: boolean
    public?: boolean
    /** 哪些角色可以看到此菜单项（不设置则所有角色可见） */
    roles?: UserRole[]
  }
}

/** 水稻模块路由 */
const riceRoutes: RouteRecordRaw[] = [
  {
    path: '/rice',
    redirect: '/rice/dashboard',
  },
  {
    path: '/rice/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/rice/dashboard/Dashboard.vue'),
    meta: { title: '数据看板', icon: 'DataAnalysis' },
  },
  {
    path: '/rice/fields',
    name: 'FieldList',
    component: () => import('@/views/rice/fields/FieldList.vue'),
    meta: { title: '地块列表', icon: 'MapLocation', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
  },
  {
    path: '/rice/fields/create',
    name: 'FieldCreate',
    component: () => import('@/views/rice/fields/FieldForm.vue'),
    meta: { title: '新增地块', hidden: true, roles: ['RICE_ADMIN'] },
  },
  {
    path: '/rice/fields/:id/edit',
    name: 'FieldEdit',
    component: () => import('@/views/rice/fields/FieldForm.vue'),
    meta: { title: '编辑地块', hidden: true, roles: ['RICE_ADMIN'] },
  },
  {
    path: '/rice/fields/detail/:id',
    name: 'FieldDetail',
    component: () => import('@/views/rice/fields/FieldDetail.vue'),
    meta: { title: '地块详情', hidden: true },
  },
  {
    path: '/rice/planting-batches',
    name: 'PlantingBatchList',
    component: () => import('@/views/rice/planting-batches/BatchList.vue'),
    meta: { title: '种植批次', icon: 'Grid', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
  },
  {
    path: '/rice/planting-batches/detail/:id',
    name: 'PlantingBatchDetail',
    component: () => import('@/views/rice/planting-batches/BatchDetail.vue'),
    meta: { title: '批次详情', hidden: true },
  },
  {
    path: '/rice/farming-logs',
    name: 'FarmingLogList',
    component: () => import('@/views/rice/farming-logs/FarmingLogList.vue'),
    meta: { title: '农事记录', icon: 'Notebook', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
  },
  {
    path: '/rice/environment-records',
    name: 'EnvironmentRecordList',
    component: () => import('@/views/rice/environment/EnvironmentList.vue'),
    meta: { title: '环境数据', icon: 'Sunny', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
  },
  {
    path: '/rice/storage-receipts',
    name: 'StorageReceiptList',
    component: () => import('@/views/rice/storage/StorageList.vue'),
    meta: { title: '收储入库', icon: 'Box', roles: ['RICE_ADMIN', 'WAREHOUSE', 'REGULATOR'] },
  },
  {
    path: '/rice/milling-batches',
    name: 'MillingBatchList',
    component: () => import('@/views/rice/milling/MillingList.vue'),
    meta: { title: '碾米加工', icon: 'Setting', roles: ['RICE_ADMIN', 'FACTORY', 'REGULATOR'] },
  },
  {
    path: '/rice/product-batches',
    name: 'ProductBatchList',
    component: () => import('@/views/rice/product/ProductList.vue'),
    meta: { title: '成品批次', icon: 'Goods', roles: ['RICE_ADMIN', 'BRAND', 'REGULATOR'] },
  },
  {
    path: '/rice/trace-codes',
    name: 'TraceCodeList',
    component: () => import('@/views/rice/trace-codes/TraceCodeList.vue'),
    meta: { title: '防伪码管理', icon: 'Ticket', roles: ['RICE_ADMIN', 'BRAND', 'REGULATOR'] },
  },
  {
    path: '/rice/channel-warnings',
    name: 'ChannelWarnings',
    component: () => import('@/views/rice/trace/ChannelWarnings.vue'),
    meta: { title: '窜货预警', icon: 'LocationInformation', roles: ['RICE_ADMIN', 'BRAND', 'REGULATOR'] },
  },
  {
    path: '/rice/regulation/risk-warnings',
    name: 'RiskWarningList',
    component: () => import('@/views/rice/regulation/RiskWarningList.vue'),
    // BRAND 无 RICE_REGULATION_VIEW 权限（其预警走「窜货预警」页），菜单不再对其显示
    meta: { title: '风险预警', icon: 'Warning', roles: ['RICE_ADMIN', 'REGULATOR'] },
  },
  {
    path: '/rice/regulation/audit-logs',
    name: 'AuditLogList',
    component: () => import('@/views/rice/regulation/AuditLogList.vue'),
    meta: { title: '审计日志', icon: 'DocumentChecked', roles: ['RICE_ADMIN', 'REGULATOR'] },
  },
  {
    path: '/rice/regulation/yield-balance',
    name: 'YieldBalance',
    component: () => import('@/views/rice/regulation/YieldBalance.vue'),
    meta: { title: '产量平衡', icon: 'DataLine', roles: ['RICE_ADMIN', 'REGULATOR'] },
  },
  {
    path: '/rice/regulation/chain-proofs',
    name: 'ChainProofManagement',
    component: () => import('@/views/rice/regulation/ChainProof.vue'),
    meta: { title: '链上存证', icon: 'Connection', roles: ['RICE_ADMIN', 'REGULATOR'] },
  },
]

/** 路由表 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/rice',
    redirect: '/rice/dashboard',
  },
  {
    path: '/',
    component: MainLayout,
    children: riceRoutes,
  },
  {
    path: '/pages/rice/trace/index',
    name: 'MiniTraceIndex',
    component: () => import('@/views/mini/TraceIndex.vue'),
    meta: { title: '扫码溯源', public: true, hidden: true },
  },
  {
    path: '/pages/rice/trace/verify',
    name: 'MiniVerifyResult',
    component: () => import('@/views/mini/VerifyResult.vue'),
    meta: { title: '真伪鉴别', public: true, hidden: true },
  },
  {
    path: '/pages/rice/trace/certificates',
    name: 'MiniCertificates',
    component: () => import('@/views/mini/Certificates.vue'),
    meta: { title: '认证证书', public: true, hidden: true },
  },
  {
    path: '/pages/rice/trace/chain-proof',
    name: 'MiniChainProof',
    component: () => import('@/views/mini/ChainProof.vue'),
    meta: { title: '链上核验', public: true, hidden: true },
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// ==================== 路由守卫 ====================
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) return next()

  // 登录页 — 已登录用户直接跳转看板
  if (to.path === '/login') {
    if (userStore.isLoggedIn) {
      return next('/rice/dashboard')
    }
    return next()
  }

  // 如果 store 中没有登录状态，尝试从 localStorage 恢复
  if (!userStore.isLoggedIn) {
    userStore.initFromStorage()
  }

  // 未登录 → 跳转登录页
  if (!userStore.isLoggedIn) {
    return next('/login')
  }

  // 检查角色是否有权访问此路由
  const routeRoles = to.meta.roles as UserRole[] | undefined
  if (routeRoles && routeRoles.length > 0 && userStore.role && !routeRoles.includes(userStore.role)) {
    // 无权限，跳转看板
    return next('/rice/dashboard')
  }

  next()
})

export default router
