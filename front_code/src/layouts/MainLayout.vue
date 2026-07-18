<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!sidebarCollapsed" class="logo-text">🌾 水稻溯源系统</span>
        <span v-else class="logo-text-mini">🌾</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="#1a5632"
        text-color="#a0c8b0"
        active-text-color="#fff"
      >
        <template v-for="item in filteredMenuItems" :key="item.path || item.group">
          <!-- 分组标题 -->
          <el-menu-item-group v-if="item.group && item.children && item.children.length > 0" :title="sidebarCollapsed ? '' : item.group">
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-menu-item-group>
          <!-- 单独菜单项 -->
          <el-menu-item v-else-if="!item.group" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar" :size="20">
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/rice/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="user-name">{{ userStore.userName }}</span>
          <el-tag type="primary" size="small">{{ userStore.roleInfo?.label }}</el-tag>
          <el-button type="danger" link size="small" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon> 退出
          </el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Fold, Expand, SwitchButton } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import type { UserRole } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => route.meta?.title as string || '')

function toggleSidebar() {
  appStore.toggleSidebar()
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }).catch(() => {})
}

/** 完整菜单配置（含角色限制） */
interface MenuChild {
  path: string
  title: string
  icon: string
  roles?: UserRole[]
}

interface MenuItem {
  group?: string
  path?: string
  title?: string
  icon?: string
  roles?: UserRole[]
  children?: MenuChild[]
}

const allMenuItems: MenuItem[] = [
  {
    group: '主业务流程',
    children: [
      { path: '/rice/dashboard', title: '数据看板', icon: 'DataAnalysis' },
      { path: '/rice/fields', title: '地块档案', icon: 'MapLocation', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
      { path: '/rice/planting-batches', title: '种植批次', icon: 'Grid', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
      { path: '/rice/farming-logs', title: '农事记录', icon: 'Notebook', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
      { path: '/rice/environment-records', title: '环境数据', icon: 'Sunny', roles: ['RICE_ADMIN', 'FARMER', 'REGULATOR'] },
      { path: '/rice/storage-receipts', title: '收储入库', icon: 'Box', roles: ['RICE_ADMIN', 'WAREHOUSE', 'REGULATOR'] },
      { path: '/rice/milling-batches', title: '碾米加工', icon: 'Setting', roles: ['RICE_ADMIN', 'FACTORY', 'REGULATOR'] },
      { path: '/rice/product-batches', title: '成品批次', icon: 'Goods', roles: ['RICE_ADMIN', 'BRAND', 'REGULATOR'] },
    ],
  },
  {
    group: '监管与风控',
    children: [
      { path: '/rice/trace-codes', title: '防伪码管理', icon: 'Ticket', roles: ['RICE_ADMIN', 'BRAND', 'REGULATOR'] },
      { path: '/rice/regulation/risk-warnings', title: '风险预警', icon: 'Warning', roles: ['RICE_ADMIN', 'BRAND', 'REGULATOR'] },
      { path: '/rice/regulation/audit-logs', title: '审计日志', icon: 'DocumentChecked', roles: ['RICE_ADMIN', 'REGULATOR'] },
      { path: '/rice/regulation/yield-balance', title: '产量平衡', icon: 'DataLine', roles: ['RICE_ADMIN', 'REGULATOR'] },
    ],
  },
]

/**
 * 判断菜单项（或子菜单）是否对当前角色可见
 */
function isVisibleForRole(item: { roles?: UserRole[] }): boolean {
  // 没有设置 roles 表示所有角色可见
  if (!item.roles || item.roles.length === 0) return true
  return item.roles.includes(userStore.role)
}

/**
 * 过滤后的菜单（根据当前用户角色）
 */
const filteredMenuItems = computed(() => {
  return allMenuItems
    .map((group) => {
      if (group.children) {
        const visibleChildren = group.children.filter((child) => isVisibleForRole(child))
        return { ...group, children: visibleChildren }
      }
      return group
    })
    .filter((group) => {
      if (group.children) {
        return group.children.length > 0
      }
      return isVisibleForRole(group as { roles?: UserRole[] })
    })
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #1a5632;
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-text {
  white-space: nowrap;
}

.logo-text-mini {
  font-size: 22px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
  padding: 0 24px;
  height: 48px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  cursor: pointer;
  color: #86909c;
}

.collapse-btn:hover {
  color: #1d2129;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 13px;
  color: #4e5969;
}

.main-content {
  background: #f5f6f8;
  min-height: calc(100vh - 48px);
  padding: 20px 24px;
}
</style>
