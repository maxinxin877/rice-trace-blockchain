<template>
  <PageContainer title="收储入库">
    <template #actions>
      <el-button type="primary" @click="showForm = true">
        <el-icon><Plus /></el-icon> 新增入库单
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
      <template #moisturePercent="{ row }">
        <span :style="{ color: row.moisturePercent > 40 || row.moisturePercent < 0 ? '#E6A23C' : '' }">
          {{ row.moisturePercent }}%
        </span>
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="openQuality(row)">质检</el-button>
      </template>
    </DataTable>

    <!-- 新增入库单弹窗 -->
    <el-dialog v-model="showForm" title="新增入库单" width="650px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="种植批次" prop="plantingBatchId">
              <el-input v-model="form.plantingBatchId" placeholder="如: RPB202607010001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原粮批次ID" prop="grainBatchId">
              <el-input v-model="form.grainBatchId" placeholder="如: GRAIN20260920001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓库ID" prop="warehouseId">
              <el-input v-model="form.warehouseId" placeholder="仓库ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入仓编号" prop="warehouseCode">
              <el-input v-model="form.warehouseCode" placeholder="如: WC-01-001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="收割面积(亩)" prop="harvestAreaMu">
              <el-input-number v-model="form.harvestAreaMu" :min="0.01" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="湿谷重量(kg)" prop="wetGrainWeightKg">
              <el-input-number v-model="form.wetGrainWeightKg" :min="0.01" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="水分(%)" prop="moisturePercent">
              <el-input-number v-model="form.moisturePercent" :min="0" :max="40" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="杂质(%)" prop="impurityPercent">
              <el-input-number v-model="form.impurityPercent" :min="0" :max="20" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="粮食等级" prop="grainGrade">
              <el-select v-model="form.grainGrade" style="width: 100%">
                <el-option label="一等" value="一等" />
                <el-option label="二等" value="二等" />
                <el-option label="三等" value="三等" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="入库时间" prop="storageTime">
          <el-date-picker v-model="form.storageTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓温(℃)">
              <el-input-number v-model="form.temperature" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓湿(%)">
              <el-input-number v-model="form.humidity" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 质检弹窗 -->
    <el-dialog v-model="showQuality" title="提交入库质检" width="700px">
      <el-form ref="qualityFormRef" :model="qualityForm" label-width="110px">
        <el-form-item label="入库单ID">
          <el-input :model-value="currentStorageId" disabled />
        </el-form-item>
        <QualityForm v-model="qualityForm" />
      </el-form>
      <template #footer>
        <el-button @click="showQuality = false">取消</el-button>
        <el-button type="primary" @click="submitQuality" :loading="submittingQuality">提交质检</el-button>
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
import QualityForm from '@/components/rice/QualityForm.vue'
import type { QualityFormData } from '@/components/rice/QualityForm.vue'
import { storageReceiptApi } from '@/api/modules/storageReceipt'
import { qualityTestApi } from '@/api/modules/qualityTest'
import { useTable } from '@/composables/useTable'
import { CHAIN_STATUS_MAP } from '@/utils/constants'
import type { RiceStorageReceipt } from '@/types/storageReceipt'

const searchItems: SearchItem[] = [
  { prop: 'grainBatchId', label: '原粮批次', type: 'input', placeholder: '输入原粮批次ID' },
  { prop: 'chainStatus', label: '链上状态', type: 'select', options: Object.entries(CHAIN_STATUS_MAP).map(([value, item]) => ({ value, label: item.label })) },
]

const columns: TableColumn[] = [
  { prop: 'storageReceiptId', label: '入库单ID', width: 180 },
  { prop: 'grainBatchId', label: '原粮批次', width: 160 },
  { prop: 'warehouseCode', label: '入仓编号', width: 120 },
  { prop: 'harvestAreaMu', label: '收割面积(亩)', width: 120 },
  { prop: 'wetGrainWeightKg', label: '湿谷重量(kg)', width: 130 },
  { prop: 'moisturePercent', label: '水分(%)', width: 90, slot: 'moisturePercent' },
  { prop: 'impurityPercent', label: '杂质(%)', width: 90 },
  { prop: 'grainGrade', label: '等级', width: 70 },
  { prop: 'chainStatus', label: '链上状态', width: 90, slot: 'chainStatus' },
]

const fetchFn = (params: Record<string, unknown>) =>
  storageReceiptApi.getList(params) as Promise<{ data: { records: RiceStorageReceipt[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RiceStorageReceipt>(fetchFn)

onMounted(() => loadData())

// 新增入库单
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  plantingBatchId: '',
  grainBatchId: '',
  warehouseId: '',
  warehouseCode: '',
  harvestAreaMu: 0,
  wetGrainWeightKg: 0,
  moisturePercent: 0,
  impurityPercent: 0,
  grainGrade: '一等',
  storageTime: '',
  temperature: undefined as number | undefined,
  humidity: undefined as number | undefined,
})

const rules: FormRules = {
  plantingBatchId: [{ required: true, message: '请输入种植批次', trigger: 'blur' }],
  grainBatchId: [{ required: true, message: '请输入原粮批次ID', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请输入仓库ID', trigger: 'blur' }],
  warehouseCode: [{ required: true, message: '请输入入仓编号', trigger: 'blur' }],
  harvestAreaMu: [{ required: true, message: '请输入收割面积', trigger: 'blur' }],
  wetGrainWeightKg: [{ required: true, message: '请输入湿谷重量', trigger: 'blur' }],
  moisturePercent: [{ required: true, message: '请输入水分', trigger: 'blur' }],
  impurityPercent: [{ required: true, message: '请输入杂质', trigger: 'blur' }],
  grainGrade: [{ required: true, message: '请选择等级', trigger: 'change' }],
  storageTime: [{ required: true, message: '请选择入库时间', trigger: 'change' }],
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await storageReceiptApi.create({ ...form } as never)
    ElMessage.success('入库单创建成功')
    showForm.value = false
    loadData()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}

// 质检
const showQuality = ref(false)
const submittingQuality = ref(false)
const currentStorageId = ref('')
const qualityFormRef = ref<FormInstance>()
const qualityForm = ref<QualityFormData>({
  testAgency: '',
  testTime: '',
  overallResult: 'PASS',
  remark: '',
  items: [],
})

function openQuality(row: RiceStorageReceipt) {
  currentStorageId.value = row.storageReceiptId
  qualityForm.value = {
    testAgency: '',
    testTime: '',
    overallResult: 'PASS',
    remark: '',
    items: [],
  }
  showQuality.value = true
}

async function submitQuality() {
  submittingQuality.value = true
  try {
    await qualityTestApi.create({
      businessType: 'STORAGE' as never,
      businessId: currentStorageId.value,
      testAgency: qualityForm.value.testAgency,
      testTime: qualityForm.value.testTime,
      overallResult: qualityForm.value.overallResult as never,
      remark: qualityForm.value.remark,
      items: qualityForm.value.items as never[],
    })
    ElMessage.success('质检提交成功')
    showQuality.value = false
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submittingQuality.value = false
  }
}
</script>
