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
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { RISK_WARNING_TYPE_MAP, RISK_LEVEL_MAP } from '@/utils/constants'

interface RiskWarning {
  warningId: string; warningType: string; riskLevel: string; businessType: string; businessId: string
  warningContent: string; handled: boolean; handledBy: string | null; handledAt: string | null; handleResult: string | null; createdAt: string
}

const mockWarnings: RiskWarning[] = [
  { warningId: 'WARN001', warningType: 'CHANNEL_CONFLICT', riskLevel: 'MEDIUM', businessType: 'TRACE_CODE', businessId: 'RC20261001000002', warningContent: '扫码地区(上海市)与预期销售区域(北京、上海)基本一致，但短时间内跨城市扫码频率较高', handled: false, handledBy: null, handledAt: null, handleResult: null, createdAt: '2026-10-16 10:00:00' },
  { warningId: 'WARN002', warningType: 'REPEAT_SCAN', riskLevel: 'HIGH', businessType: 'TRACE_CODE', businessId: 'RC20261001000008', warningContent: '同一防伪码在30分钟内被扫码312次，疑似批量复制攻击', handled: false, handledBy: null, handledAt: null, handleResult: null, createdAt: '2026-10-18 17:00:00' },
  { warningId: 'WARN003', warningType: 'YIELD_BALANCE', riskLevel: 'MEDIUM', businessType: 'MILLING_BATCH', businessId: 'MB202607010002', warningContent: '精米产出率60%接近下限阈值(55%)，请核实原粮水分和加工工艺', handled: true, handledBy: 'ADMIN001', handledAt: '2026-10-04 09:00:00', handleResult: '经核实，原粮水分偏高导致产出率偏低，已在仓储环节整改', createdAt: '2026-10-03 18:00:00' },
  { warningId: 'WARN004', warningType: 'CHAIN_VERIFY_FAILED', riskLevel: 'HIGH', businessType: 'FIELD', businessId: 'FIELD202607010003', warningContent: '地块数据哈希与链上存证不一致，可能存在数据篡改', handled: false, handledBy: null, handledAt: null, handleResult: null, createdAt: '2026-10-20 08:00:00' },
  { warningId: 'WARN005', warningType: 'UNACTIVATED_SCAN', riskLevel: 'HIGH', businessType: 'TRACE_CODE', businessId: 'RC20261001000006', warningContent: '未激活的防伪码被扫码，疑似码被复制或泄露', handled: true, handledBy: 'ADMIN002', handledAt: '2026-10-17 14:00:00', handleResult: '已停用该码并启动追溯调查', createdAt: '2026-10-17 13:00:00' },
]

const searchItems: SearchItem[] = [
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
  let list = [...mockWarnings]
  if (params.warningType) list = list.filter((w) => w.warningType === params.warningType)
  if (params.riskLevel) list = list.filter((w) => w.riskLevel === params.riskLevel)
  const p = (params.page as number) || 1; const ps = (params.pageSize as number) || 10
  return { data: { records: list.slice((p - 1) * ps, (p - 1) * ps + ps), total: list.length } }
}

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RiskWarning>(fetchFn)
onMounted(() => loadData())

const showHandle = ref(false)
const currentWarning = ref<RiskWarning | null>(null)
const handleForm = reactive({ handleResult: '' })

function openHandle(row: RiskWarning) { currentWarning.value = row; handleForm.handleResult = ''; showHandle.value = true }

function submitHandle() {
  if (!handleForm.handleResult.trim()) { ElMessage.warning('请输入处理结果'); return }
  if (currentWarning.value) {
    currentWarning.value.handled = true
    currentWarning.value.handledBy = 'ADMIN001'
    currentWarning.value.handledAt = new Date().toISOString()
    currentWarning.value.handleResult = handleForm.handleResult
  }
  ElMessage.success('处理完成')
  showHandle.value = false
  loadData()
}
</script>

<style scoped>
.handled-text { color: #c9cdd4; }
</style>
