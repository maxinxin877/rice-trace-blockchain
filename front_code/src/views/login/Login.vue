<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="login-header">
        <h1>🌾 水稻产品质量安全溯源系统</h1>
        <p>管理员端登录</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号（如 admin1）"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 提示信息 -->
      <el-alert
        title="Mock 测试账号"
        type="info"
        :closable="false"
        style="margin-top: 16px"
      >
        <template #default>
          <div class="account-hints">
            <div v-for="account in MOCK_ACCOUNTS" :key="account.username" class="hint-item">
              <span class="hint-user">{{ account.username }}</span>
              <span class="hint-sep">/</span>
              <span class="hint-pass">123123123</span>
              <span class="hint-role">— {{ getRoleLabel(account.role) }}</span>
            </div>
          </div>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore, MOCK_ACCOUNTS, ROLES } from '@/stores/user'
import type { UserRole } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function getRoleLabel(code: UserRole): string {
  return ROLES.find((r) => r.code === code)?.label || code
}

function handleLogin() {
  if (!formRef.value) return
  formRef.value.validate((valid) => {
    if (!valid) return

    loading.value = true
    // 模拟登录延迟
    setTimeout(() => {
      const result = userStore.login(form.username, form.password)
      loading.value = false

      if (result.success) {
        ElMessage.success(`欢迎，${userStore.userName}（${userStore.roleInfo?.label}）`)
        router.replace('/rice/dashboard')
      } else {
        ElMessage.error(result.message)
      }
    }, 500)
  })
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eaf4ed;
  padding: 20px;
}

.login-card {
  width: 440px;
  max-width: 100%;
  background: #fff;
  border-radius: 8px;
  padding: 48px 40px 36px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1d2129;
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.login-header p {
  font-size: 13px;
  color: #86909c;
  margin: 0;
}

.account-hints {
  font-size: 12px;
  line-height: 2;
}

.hint-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.hint-user {
  color: #1d2129;
  font-weight: 600;
  font-family: monospace;
}

.hint-sep {
  color: #c9cdd4;
}

.hint-pass {
  color: #86909c;
  font-family: monospace;
}

.hint-role {
  color: #4e5969;
}
</style>
