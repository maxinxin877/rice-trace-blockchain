<template>
  <PageContainer title="防伪码管理">
    <template #actions>
      <el-button type="primary" @click="openGenerate">
        <el-icon><Plus /></el-icon> 批量生成
      </el-button>
    </template>

    <SearchForm :items="searchItems" @search="onSearch" @reset="onReset" />

    <DataTable
      :columns="columns"
      :data="data"
      :loading="loading"
      :total="total"
      :page="page"
      :pageSize="pageSize"
      @page-change="onPageChange"
    >
      <template #status="{ row }">
        <StatusTag type="traceCode" :value="row.status" />
      </template>
      <template #riskLevel="{ row }">
        <StatusTag type="risk" :value="row.riskLevel" />
      </template>
      <template #chainStatus="{ row }">
        <StatusTag type="chain" :value="row.chainStatus" />
      </template>
      <template #actions="{ row }">
        <el-button v-if="row.status === 'GENERATED'" type="success" link size="small" @click="activateCodes([row.traceCode])">激活</el-button>
        <el-button type="primary" link size="small" @click="openConsumer(row.traceCode)">扫码预览</el-button>
        <el-button v-if="row.scanCount > 0" type="warning" link size="small" @click="showScanDetail(row)">扫码记录</el-button>
      </template>
    </DataTable>

    <!-- 批量生成弹窗 -->
    <el-dialog v-model="showGenerate" title="批量生成防伪码" width="500px">
      <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="110px">
        <el-form-item label="成品批次" prop="productBatchId">
          <el-select v-model="genForm.productBatchId" filterable placeholder="请选择成品批次" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.productBatchId" :label="`${p.productName} (${p.productBatchId})`" :value="p.productBatchId" />
          </el-select>
        </el-form-item>
        <el-form-item label="生成数量" prop="quantity">
          <el-input-number v-model="genForm.quantity" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="包装规格">
          <el-input v-model="genForm.packageSpec" placeholder="如: 5kg/袋" />
        </el-form-item>
        <el-form-item label="预期销售区域">
          <el-input v-model="genForm.expectedSaleRegion" placeholder="如: 北京、上海" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerate = false">取消</el-button>
        <el-button type="primary" @click="submitGenerate" :loading="genLoading">确认生成</el-button>
      </template>
    </el-dialog>

    <!-- 扫码记录弹窗 -->
    <el-dialog v-model="showScanLog" title="扫码记录" width="600px">
      <el-table :data="currentScanLogs" border stripe size="small">
        <el-table-column prop="scanTime" label="扫码时间" width="170" />
        <el-table-column prop="region" label="地区" width="100" />
        <el-table-column label="首次扫码" width="90">
          <template #default="{ row }">{{ row.firstScan ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="scene" label="场景" width="90" />
      </el-table>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { TRACE_CODE_STATUS_MAP } from '@/utils/constants'
import { traceabilityApi } from '@/api/modules/traceability'
import { productBatchApi } from '@/api/modules/productBatch'
import type { ScanLogRecord, TraceCodeRecord } from '@/types/traceability'

const router = useRouter()

const searchItems: SearchItem[] = [
  { prop: 'traceCode', label: '防伪码', type: 'input', placeholder: '输入防伪码' },
  { prop: 'status', label: '状态', type: 'select', options: Object.entries(TRACE_CODE_STATUS_MAP).map(([v, i]) => ({ value: v, label: i.label })) },
  { prop: 'riskLevel', label: '风险等级', type: 'select', options: [{ value: 'LOW', label: '低' }, { value: 'MEDIUM', label: '中' }, { value: 'HIGH', label: '高' }] },
]

const columns: TableColumn[] = [
  { prop: 'traceCode', label: '防伪码', width: 180 },
  { prop: 'productName', label: '产品名称', minWidth: 160 },
  { prop: 'packageSpec', label: '规格', width: 100 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'scanCount', label: '扫码次数', width: 90 },
  { prop: 'riskLevel', label: '风险等级', width: 90, slot: 'riskLevel' },
  { prop: 'chainStatus', label: '链上状态', width: 80, slot: 'chainStatus' },
]

const fetchFn = async (params: Record<string, unknown>) => {
  const response = await traceabilityApi.getTraceCodes(params)
  return { data: response.data }
}

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<TraceCodeRecord>(fetchFn)

const productOptions = ref<{ productBatchId: string; productName: string }[]>([])

onMounted(async () => {
  await loadData()
  const response = await productBatchApi.getList({ page: 1, pageSize: 100 })
  productOptions.value = response.data.records.map((item) => ({ productBatchId: item.productBatchId, productName: item.productName }))
})

function openConsumer(traceCode: string) {
  router.push({ path: '/pages/rice/trace/index', query: { code: traceCode } })
}

// 生成
const showGenerate = ref(false)
const genLoading = ref(false)
const genFormRef = ref<FormInstance>()
const genForm = reactive({ productBatchId: '', quantity: 100, packageSpec: '', expectedSaleRegion: '' })
const genRules: FormRules = {
  productBatchId: [{ required: true, message: '请选择成品批次', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
}

function openGenerate() { genForm.productBatchId = ''; genForm.quantity = 100; genForm.packageSpec = ''; genForm.expectedSaleRegion = ''; showGenerate.value = true }

async function submitGenerate() {
  if (!genFormRef.value) return
  const valid = await genFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!genForm.packageSpec.trim()) { ElMessage.warning('请输入包装规格'); return }
  genLoading.value = true
  try {
    const response = await traceabilityApi.generateTraceCodes({ ...genForm, expireDays: 0 })
    ElMessage.success(`${response.message}：${response.data.quantity} 个防伪码`)
    showGenerate.value = false
    await loadData()
  } finally {
    genLoading.value = false
  }
}

// 激活
async function activateCodes(codes: string[]) {
  try {
    await ElMessageBox.confirm(`确认激活 ${codes.length} 个防伪码？`, '提示', { type: 'info' })
    const record = data.value.find((item) => item.traceCode === codes[0])
    if (!record) return
    const response = await traceabilityApi.activateTraceCodes({ productBatchId: record.productBatchId, traceCodes: codes, activatedBy: 'ADMIN001', reason: '成品批次出库上市前激活' })
    ElMessage.success(`已激活 ${response.data.activatedCount} 个防伪码`)
    await loadData()
  } catch {
    // 用户取消时无需提示
  }
}

// 扫码记录
const showScanLog = ref(false)
const currentScanLogs = ref<ScanLogRecord[]>([])

async function showScanDetail(row: TraceCodeRecord) {
  currentScanLogs.value = (await traceabilityApi.getScanLogs(row.traceCode)).data
  showScanLog.value = true
}
</script>
