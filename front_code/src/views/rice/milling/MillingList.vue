<template>
  <PageContainer title="碾米加工">
    <template #actions>
      <el-button type="primary" @click="showCreateForm = true">
        <el-icon><Plus /></el-icon> 创建加工批次
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
      <template #chainStatus="{ row }">
        <StatusTag type="chain" :value="row.chainStatus" />
      </template>
      <template #yieldRate="{ row }">
        <span :style="{ color: row.yieldRate && (row.yieldRate < 55 || row.yieldRate > 75) ? '#E6A23C' : '' }">
          {{ row.yieldRate != null ? row.yieldRate + '%' : '-' }}
        </span>
      </template>
      <template #actions="{ row }">
        <el-button type="info" link size="small" @click="openDetail(row)">详情</el-button>
        <el-button type="primary" link size="small" @click="openProcessParams(row)">工艺参数</el-button>
        <el-button v-if="!row.processEndTime" type="success" link size="small" @click="openComplete(row)">完成加工</el-button>
      </template>
    </DataTable>

    <!-- 创建加工批次弹窗 -->
    <el-dialog v-model="showCreateForm" title="创建加工批次" width="650px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="130px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="原粮批次" prop="grainBatchId">
              <el-select v-model="createForm.grainBatchId" placeholder="请选择原粮批次" filterable style="width: 100%">
                <el-option
                  v-for="r in receiptOptions"
                  :key="r.grainBatchId"
                  :label="`${r.grainBatchId}（${r.warehouseCode}）`"
                  :value="r.grainBatchId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成品批次ID" prop="productBatchId">
              <el-input v-model="createForm.productBatchId" placeholder="自定义成品批次ID，如: PROD20261001001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工厂ID" prop="factoryId">
              <el-input v-model="createForm.factoryId" placeholder="工厂ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原粮出库量(kg)" prop="grainOutWeightKg">
              <el-input-number v-model="createForm.grainOutWeightKg" :min="0.01" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="加工开始时间" prop="processStartTime">
          <el-date-picker v-model="createForm.processStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-divider content-position="left">加工工艺参数</el-divider>
        <ProcessParamsForm v-model="createForm.processParams" />
      </el-form>
      <template #footer>
        <el-button @click="showCreateForm = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="creating">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 工艺参数查看弹窗 -->
    <el-dialog v-model="showParamsDialog" title="加工工艺参数" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="脱壳">{{ currentParams?.husking || '-' }}</el-descriptions-item>
        <el-descriptions-item label="抛光">{{ currentParams?.polishing || '-' }}</el-descriptions-item>
        <el-descriptions-item label="色选">{{ currentParams?.colorSorting || '-' }}</el-descriptions-item>
        <el-descriptions-item label="包装">{{ currentParams?.packaging || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 加工批次详情弹窗 -->
    <el-dialog v-model="showDetail" title="加工批次详情" width="640px">
      <el-descriptions v-if="currentDetail" :column="2" border>
        <el-descriptions-item label="加工批次ID" :span="2">{{ currentDetail.millingBatchId }}</el-descriptions-item>
        <el-descriptions-item label="原粮批次">{{ currentDetail.grainBatchId }}</el-descriptions-item>
        <el-descriptions-item label="成品批次">{{ currentDetail.productBatchId }}</el-descriptions-item>
        <el-descriptions-item label="工厂ID">{{ currentDetail.factoryId }}</el-descriptions-item>
        <el-descriptions-item label="原粮出库量(kg)">{{ currentDetail.grainOutWeightKg }}</el-descriptions-item>
        <el-descriptions-item label="精米产出量(kg)">{{ currentDetail.riceOutputWeightKg ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="产出率(%)">{{ currentDetail.yieldRate != null ? currentDetail.yieldRate + '%' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="加工开始时间">{{ currentDetail.processStartTime }}</el-descriptions-item>
        <el-descriptions-item label="加工结束时间">{{ currentDetail.processEndTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="质量摘要" :span="2">{{ currentDetail.qualitySummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链上状态">{{ currentDetail.chainStatus }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentDetail.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 完成加工弹窗 -->
    <el-dialog v-model="showCompleteForm" title="完成加工" width="500px">
      <el-form ref="completeFormRef" :model="completeForm" :rules="completeRules" label-width="140px">
        <el-form-item label="加工批次ID">
          <el-input :model-value="completeForm.millingBatchId" disabled />
        </el-form-item>
        <el-form-item label="精米产出量(kg)" prop="riceOutputWeightKg">
          <el-input-number v-model="completeForm.riceOutputWeightKg" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="加工结束时间" prop="processEndTime">
          <el-date-picker v-model="completeForm.processEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="质量摘要">
          <el-input v-model="completeForm.qualitySummary" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCompleteForm = false">取消</el-button>
        <el-button type="primary" @click="submitComplete" :loading="completing">确认完成</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import ProcessParamsForm from '@/components/rice/ProcessParamsForm.vue'
import { millingBatchApi } from '@/api/modules/millingBatch'
import { storageReceiptApi } from '@/api/modules/storageReceipt'
import { useTable } from '@/composables/useTable'
import type { RiceMillingBatch, MillingProcessParams } from '@/types/millingBatch'

const searchItems: SearchItem[] = [
  { prop: 'grainBatchId', label: '原粮批次', type: 'input', placeholder: '输入原粮批次ID' },
  { prop: 'productBatchId', label: '成品批次', type: 'input', placeholder: '输入成品批次ID' },
]

const columns: TableColumn[] = [
  { prop: 'millingBatchId', label: '加工批次ID', width: 180 },
  { prop: 'grainBatchId', label: '原粮批次', width: 160 },
  { prop: 'productBatchId', label: '成品批次', width: 160 },
  { prop: 'grainOutWeightKg', label: '出库量(kg)', width: 120 },
  { prop: 'riceOutputWeightKg', label: '产出量(kg)', width: 120 },
  { prop: 'yieldRate', label: '产出率', width: 90, slot: 'yieldRate' },
  { prop: 'chainStatus', label: '链上状态', width: 90, slot: 'chainStatus' },
]

const fetchFn = (params: Record<string, unknown>) =>
  millingBatchApi.getList(params) as Promise<{ data: { records: RiceMillingBatch[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RiceMillingBatch>(fetchFn)

// 原粮批次选项（来自入库单）
const receiptOptions = ref<{ grainBatchId: string; warehouseCode: string }[]>([])

onMounted(async () => {
  loadData()
  const res = await storageReceiptApi.getList({ pageSize: 100 })
  if (res.code === 200) {
    receiptOptions.value = res.data.records.map((r) => ({
      grainBatchId: r.grainBatchId,
      warehouseCode: r.warehouseCode,
    }))
  }
})

// 创建
const showCreateForm = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  grainBatchId: '',
  productBatchId: '',
  factoryId: '',
  grainOutWeightKg: 0,
  processStartTime: '',
  processParams: {} as MillingProcessParams,
})

const createRules: FormRules = {
  grainBatchId: [{ required: true, message: '请输入原粮批次', trigger: 'blur' }],
  productBatchId: [{ required: true, message: '请输入成品批次', trigger: 'blur' }],
  factoryId: [{ required: true, message: '请输入工厂ID', trigger: 'blur' }],
  grainOutWeightKg: [{ required: true, message: '请输入出库量', trigger: 'blur' }],
  processStartTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
}

async function submitCreate() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await millingBatchApi.create({ ...createForm } as never)
    ElMessage.success('加工批次创建成功')
    showCreateForm.value = false
    loadData()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

// 详情
const showDetail = ref(false)
const currentDetail = ref<RiceMillingBatch | null>(null)

async function openDetail(row: RiceMillingBatch) {
  try {
    const res = await millingBatchApi.getById(row.millingBatchId)
    currentDetail.value = res.data
    showDetail.value = true
  } catch {
    ElMessage.error('获取详情失败')
  }
}

// 工艺参数弹窗
const showParamsDialog = ref(false)
const currentParams = ref<MillingProcessParams | null>(null)

function openProcessParams(row: RiceMillingBatch) {
  currentParams.value = row.processParams
  showParamsDialog.value = true
}

// 完成加工
const showCompleteForm = ref(false)
const completing = ref(false)
const completeFormRef = ref<FormInstance>()
const completeForm = reactive({
  millingBatchId: '',
  riceOutputWeightKg: 0,
  processEndTime: '',
  qualitySummary: '',
  processParams: {} as MillingProcessParams,
})

const completeRules: FormRules = {
  riceOutputWeightKg: [{ required: true, message: '请输入精米产出量', trigger: 'blur' }],
  processEndTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

function openComplete(row: RiceMillingBatch) {
  completeForm.millingBatchId = row.millingBatchId
  completeForm.riceOutputWeightKg = 0
  completeForm.processEndTime = ''
  completeForm.qualitySummary = ''
  completeForm.processParams = row.processParams || {}
  showCompleteForm.value = true
}

async function submitComplete() {
  if (!completeFormRef.value) return
  const valid = await completeFormRef.value.validate().catch(() => false)
  if (!valid) return
  completing.value = true
  try {
    await millingBatchApi.complete({ ...completeForm } as never)
    ElMessage.success('加工完成')
    showCompleteForm.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    completing.value = false
  }
}
</script>
