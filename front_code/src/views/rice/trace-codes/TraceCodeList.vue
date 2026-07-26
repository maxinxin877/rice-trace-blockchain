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
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { TRACE_CODE_STATUS_MAP } from '@/utils/constants'
import { productBatchApi } from '@/api/modules/productBatch'

interface TraceCode {
  traceCode: string
  productBatchId: string
  productName: string
  packageSpec: string
  status: string
  scanCount: number
  firstScannedAt: string | null
  riskLevel: string
  chainStatus: string
  expectedSaleRegion: string
  createdAt: string
}

interface ScanLogItem {
  scanTime: string
  region: string
  firstScan: boolean
  scene: string
}

// Mock 数据
const mockCodes: TraceCode[] = [
  { traceCode: 'RC20261001000001', productBatchId: 'PROD20261001001', productName: '五常有机稻花香大米', packageSpec: '5kg/袋', status: 'ACTIVATED', scanCount: 128, firstScannedAt: '2026-10-15 14:23:00', riskLevel: 'LOW', chainStatus: 'SUCCESS', expectedSaleRegion: '北京、上海', createdAt: '2026-10-01' },
  { traceCode: 'RC20261001000002', productBatchId: 'PROD20261001001', productName: '五常有机稻花香大米', packageSpec: '5kg/袋', status: 'ACTIVATED', scanCount: 45, firstScannedAt: '2026-10-16 09:12:00', riskLevel: 'MEDIUM', chainStatus: 'SUCCESS', expectedSaleRegion: '北京、上海', createdAt: '2026-10-01' },
  { traceCode: 'RC20261001000003', productBatchId: 'PROD20261001001', productName: '五常有机稻花香大米', packageSpec: '5kg/袋', status: 'GENERATED', scanCount: 0, firstScannedAt: null, riskLevel: 'LOW', chainStatus: 'PENDING', expectedSaleRegion: '北京、上海', createdAt: '2026-10-01' },
  { traceCode: 'RC20261001000004', productBatchId: 'PROD20261001001', productName: '五常有机稻花香大米', packageSpec: '5kg/袋', status: 'GENERATED', scanCount: 0, firstScannedAt: null, riskLevel: 'LOW', chainStatus: 'PENDING', expectedSaleRegion: '北京、上海', createdAt: '2026-10-01' },
  { traceCode: 'RC20261001000005', productBatchId: 'PROD20261001002', productName: '五常大米（优质）', packageSpec: '10kg/袋', status: 'ACTIVATED', scanCount: 3, firstScannedAt: '2026-10-20 11:00:00', riskLevel: 'LOW', chainStatus: 'SUCCESS', expectedSaleRegion: '东北三省', createdAt: '2026-10-03' },
  { traceCode: 'RC20261001000006', productBatchId: 'PROD20261001002', productName: '五常大米（优质）', packageSpec: '10kg/袋', status: 'DEACTIVATED', scanCount: 0, firstScannedAt: null, riskLevel: 'HIGH', chainStatus: 'SUCCESS', expectedSaleRegion: '东北三省', createdAt: '2026-10-03' },
  { traceCode: 'RC20261001000007', productBatchId: 'PROD20261001003', productName: '稻花香2号精米', packageSpec: '2.5kg/盒', status: 'ACTIVATED', scanCount: 67, firstScannedAt: '2026-10-12 08:30:00', riskLevel: 'LOW', chainStatus: 'PENDING', expectedSaleRegion: '全国', createdAt: '2026-10-05' },
  { traceCode: 'RC20261001000008', productBatchId: 'PROD20261001003', productName: '稻花香2号精米', packageSpec: '2.5kg/盒', status: 'RISK', scanCount: 312, firstScannedAt: '2026-10-12 08:30:00', riskLevel: 'HIGH', chainStatus: 'SUCCESS', expectedSaleRegion: '全国', createdAt: '2026-10-05' },
]

const mockScanLogs: ScanLogItem[] = [
  { scanTime: '2026-10-15 14:23:00', region: '北京市朝阳区', firstScan: true, scene: '扫码溯源' },
  { scanTime: '2026-10-16 09:12:00', region: '上海市浦东新区', firstScan: false, scene: '扫码溯源' },
  { scanTime: '2026-10-18 16:45:00', region: '深圳市南山区', firstScan: false, scene: '扫码溯源' },
]

const searchItems: SearchItem[] = [
  { prop: 'traceCode', label: '防伪码', type: 'input', placeholder: '输入防伪码' },
  { prop: 'status', label: '状态', type: 'select', options: Object.entries(TRACE_CODE_STATUS_MAP).map(([v, i]) => ({ value: v, label: i.label })) },
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
  let list = [...mockCodes]
  if (params.traceCode) list = list.filter((c) => c.traceCode.includes(params.traceCode as string))
  if (params.status) list = list.filter((c) => c.status === params.status)
  const page = (params.page as number) || 1
  const pageSize = (params.pageSize as number) || 10
  const start = (page - 1) * pageSize
  return { data: { records: list.slice(start, start + pageSize), total: list.length } }
}

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<TraceCode>(fetchFn)

const productOptions = ref<{ productBatchId: string; productName: string }[]>([])

onMounted(async () => {
  loadData()
  const res = await productBatchApi.getList({ pageSize: 100 })
  if (res.code === 200) productOptions.value = res.data.records.map((p) => ({ productBatchId: p.productBatchId, productName: p.productName }))
})

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
  genLoading.value = true
  setTimeout(() => {
    for (let i = 0; i < genForm.quantity; i++) {
      mockCodes.push({
        traceCode: `RC${Date.now().toString().slice(2)}${String(i).padStart(4, '0')}`,
        productBatchId: genForm.productBatchId,
        productName: productOptions.value.find((p) => p.productBatchId === genForm.productBatchId)?.productName || '',
        packageSpec: genForm.packageSpec || '5kg/袋',
        status: 'GENERATED', scanCount: 0, firstScannedAt: null,
        riskLevel: 'LOW', chainStatus: 'PENDING', expectedSaleRegion: genForm.expectedSaleRegion,
        createdAt: new Date().toISOString().slice(0, 10),
      })
    }
    ElMessage.success(`成功生成 ${genForm.quantity} 个防伪码`)
    showGenerate.value = false
    genLoading.value = false
    loadData()
  }, 600)
}

// 激活
function activateCodes(codes: string[]) {
  ElMessageBox.confirm(`确认激活 ${codes.length} 个防伪码？`, '提示', { type: 'info' }).then(() => {
    codes.forEach((c) => {
      const found = mockCodes.find((m) => m.traceCode === c)
      if (found && found.status === 'GENERATED') found.status = 'ACTIVATED'
    })
    ElMessage.success('激活成功')
    loadData()
  }).catch(() => {})
}

// 扫码记录
const showScanLog = ref(false)
const currentScanLogs = ref<ScanLogItem[]>([])

function showScanDetail(_row: TraceCode) {
  currentScanLogs.value = mockScanLogs
  showScanLog.value = true
}
</script>
