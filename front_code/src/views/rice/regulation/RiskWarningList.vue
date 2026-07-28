<template>
  <PageContainer title="风险预警">
    <SearchForm :items="searchItems" @search="onSearch" @reset="onReset" />

    <DataTable :columns="columns" :data="data" :loading="loading" :total="total" :page="page" :pageSize="pageSize" @page-change="onPageChange">
      <template #riskLevel="{ row }"><StatusTag type="risk" :value="row.riskLevel" /></template>
      <template #warningType="{ row }">{{ RISK_WARNING_TYPE_MAP[row.warningType] || row.warningType }}</template>
      <template #handled="{ row }">{{ row.handled ? '已处理' : '待处理' }}</template>
      <template #actions="{ row }">
        <el-button v-if="!row.handled" type="primary" link size="small" @click="openHandle(row)">处理</el-button>
        <span v-else class="handled-text">—</span>
      </template>
    </DataTable>

    <!-- 处理弹窗 -->
    <el-dialog v-model="showHandle" title="处理风险预警" width="550px">
      <el-descriptions :column="1" border style="margin-bottom: 16px">
        <el-descriptions-item label="预警类型">{{ RISK_WARNING_TYPE_MAP[currentWarning?.warningType || ''] }}</el-descriptions-item>
        <el-descriptions-item label="风险等级"><StatusTag type="risk" :value="currentWarning?.riskLevel || ''" /></el-descriptions-item>
        <el-descriptions-item label="关联业务">{{ currentWarning?.businessType }} / {{ currentWarning?.businessId }}</el-descriptions-item>
        <el-descriptions-item label="预警内容">{{ currentWarning?.warningContent }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentWarning?.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="处理结果" required>
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" placeholder="请输入处理结果说明" />
        </el-form-item>
        <el-form-item label="处理原因" required>
          <el-input v-model="handleForm.reason" type="textarea" :rows="2" placeholder="请输入处置原因，将同步写入审计日志" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandle = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { RISK_WARNING_TYPE_MAP, RISK_LEVEL_MAP } from '@/utils/constants'
import { regulationApi } from '@/api/modules/regulation'
import type { RiskWarningRecord } from '@/types/regulation'

const searchItems: SearchItem[] = [
  { prop: 'businessId', label: '关联业务', type: 'input', placeholder: '请输入防伪码或业务ID' },
  { prop: 'warningType', label: '预警类型', type: 'select', options: Object.entries(RISK_WARNING_TYPE_MAP).map(([v, l]) => ({ value: v, label: l })) },
  { prop: 'riskLevel', label: '风险等级', type: 'select', options: Object.entries(RISK_LEVEL_MAP).map(([v, i]) => ({ value: v, label: i.label })) },
]

const columns: TableColumn[] = [
  { prop: 'warningId', label: '预警ID', width: 100 },
  { prop: 'warningType', label: '预警类型', width: 120, slot: 'warningType' },
  { prop: 'riskLevel', label: '风险等级', width: 90, slot: 'riskLevel' },
  { prop: 'businessId', label: '关联业务', width: 180 },
  { prop: 'warningContent', label: '预警内容', minWidth: 280 },
  { prop: 'handled', label: '状态', width: 80, slot: 'handled' },
  { prop: 'createdAt', label: '创建时间', width: 170 },
]

const fetchFn = async (params: Record<string, unknown>) => {
  const response = await regulationApi.getRiskWarnings(params)
  return { data: response.data }
}

const route = useRoute()
const { loading, data, total, page, pageSize, query, loadData, onSearch, onReset, onPageChange } = useTable<RiskWarningRecord>(fetchFn)

const showHandle = ref(false)
const currentWarning = ref<RiskWarningRecord | null>(null)
const handleForm = reactive({ handleResult: '', reason: '' })

function openHandle(row: RiskWarningRecord) { currentWarning.value = row; handleForm.handleResult = ''; handleForm.reason = ''; showHandle.value = true }

onMounted(async () => {
  const businessId = String(route.query.businessId || '')
  if (businessId) query.businessId = businessId
  await loadData()
  const target = data.value.find((item) => item.businessId === businessId && !item.handled)
  if (target) openHandle(target)
})

async function submitHandle() {
  if (!handleForm.handleResult.trim()) { ElMessage.warning('请输入处理结果'); return }
  if (!handleForm.reason.trim()) { ElMessage.warning('请输入处理原因'); return }
  if (!currentWarning.value) return
  const response = await regulationApi.handleRiskWarning(currentWarning.value.warningId, { ...handleForm })
  ElMessage.success(response.message)
  showHandle.value = false
  await loadData()
}
</script>

<style scoped>
.handled-text { color: #c9cdd4; }
</style>
