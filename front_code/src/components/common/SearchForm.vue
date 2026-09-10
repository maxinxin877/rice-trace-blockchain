<template>
  <el-form :model="formData" inline class="search-form">
    <el-form-item
      v-for="item in visibleItems"
      :key="item.prop"
      :label="item.label"
    >
      <!-- 输入框 -->
      <el-input
        v-if="item.type === 'input'"
        v-model="formData[item.prop]"
        :placeholder="item.placeholder || '请输入'"
        clearable
        style="width: 180px"
      />
      <!-- 下拉选择 -->
      <el-select
        v-else-if="item.type === 'select'"
        v-model="formData[item.prop]"
        :placeholder="item.placeholder || '请选择'"
        clearable
        style="width: 180px"
      >
        <el-option
          v-for="opt in item.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <!-- 日期范围 -->
      <el-date-picker
        v-else-if="item.type === 'daterange'"
        v-model="formData[item.prop]"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
      />
      <!-- 级联选择（省市区） -->
      <el-cascader
        v-else-if="item.type === 'cascader'"
        v-model="formData[item.prop]"
        :options="item.options"
        :placeholder="item.placeholder || '请选择'"
        clearable
        style="width: 180px"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button
        v-if="hasMore"
        type="primary"
        link
        @click="expanded = !expanded"
      >
        {{ expanded ? '收起' : '展开' }}
        <el-icon><ArrowUp v-if="expanded" /><ArrowDown v-else /></el-icon>
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

export interface SearchItem {
  prop: string
  label: string
  type: 'input' | 'select' | 'daterange' | 'cascader'
  placeholder?: string
  options?: { label: string; value: string; children?: { label: string; value: string; children?: { label: string; value: string }[] }[] }[]
}

const props = defineProps<{
  items: SearchItem[]
  defaultExpand?: number
}>()

const emit = defineEmits<{
  search: [data: Record<string, unknown>]
  reset: []
}>()

const expanded = ref(false)
const defaultExpand = props.defaultExpand ?? 3

const hasMore = computed(() => props.items.length > defaultExpand)

const visibleItems = computed(() => {
  if (!hasMore.value || expanded.value) return props.items
  return props.items.slice(0, defaultExpand)
})

// 动态 formData
const formData = ref<Record<string, unknown>>({})
// 初始化空值
props.items.forEach((item) => {
  formData.value[item.prop] = undefined
})

function handleSearch() {
  // 过滤空值
  const data: Record<string, unknown> = {}
  Object.entries(formData.value).forEach(([key, val]) => {
    if (val !== undefined && val !== '' && val !== null) {
      data[key] = val
    }
  })
  emit('search', data)
}

function handleReset() {
  props.items.forEach((item) => {
    formData.value[item.prop] = undefined
  })
  emit('reset')
}
</script>

<style scoped>
.search-form {
  padding: 14px 16px;
  background: #f7f8fa;
  border-radius: 6px;
  margin-bottom: 16px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 8px;
}
</style>
