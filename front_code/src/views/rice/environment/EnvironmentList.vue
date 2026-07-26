<template>
  <PageContainer title="环境数据">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增环境记录
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
      <template #sourceType="{ row }">
        <StatusTag type="environment" :value="row.sourceType" />
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="$router.push(`/rice/planting-batches/detail/${row.plantingBatchId}`)">
          查看批次
        </el-button>
      </template>
    </DataTable>

    <!-- 新增环境记录弹窗 -->
    <el-dialog v-model="showForm" title="新增环境记录" width="550px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="关联批次" prop="plantingBatchId">
          <el-select v-model="form.plantingBatchId" placeholder="请选择种植批次" filterable style="width: 100%">
            <el-option
              v-for="b in batchOptions"
              :key="b.plantingBatchId"
              :label="`${b.plantingBatchId} — ${b.riceVariety}`"
              :value="b.plantingBatchId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据来源" prop="sourceType">
          <el-select v-model="form.sourceType" style="width: 100%">
            <el-option
              v-for="(item, value) in ENVIRONMENT_SOURCE_TYPE_MAP"
              :key="value"
              :label="item.label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="记录时间" prop="recordTime">
          <el-date-picker v-model="form.recordTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="气温(℃)">
              <el-input-number v-model="form.airTemperature" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="空气湿度(%)">
              <el-input-number v-model="form.airHumidity" :precision="2" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="土壤湿度(%)">
              <el-input-number v-model="form.soilMoisture" :precision="2" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="降雨量(mm)">
              <el-input-number v-model="form.rainfall" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="风速(m/s)">
          <el-input-number v-model="form.windSpeed" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认</el-button>
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
import { environmentRecordApi } from '@/api/modules/environmentRecord'
import { plantingBatchApi } from '@/api/modules/plantingBatch'
import { useTable } from '@/composables/useTable'
import { ENVIRONMENT_SOURCE_TYPE_MAP } from '@/utils/constants'
import { mockId } from '@/api/mock'
import { mockEnvironmentRecords } from '@/api/mock/data/environmentRecords'
import type { RiceEnvironmentRecord } from '@/types/environmentRecord'

const searchItems: SearchItem[] = [
  { prop: 'plantingBatchId', label: '种植批次', type: 'input', placeholder: '输入批次ID' },
  { prop: 'sourceType', label: '数据来源', type: 'select', options: Object.entries(ENVIRONMENT_SOURCE_TYPE_MAP).map(([value, item]) => ({ value, label: item.label })) },
]

const columns: TableColumn[] = [
  { prop: 'environmentRecordId', label: '记录ID', width: 200 },
  { prop: 'plantingBatchId', label: '关联批次', width: 180 },
  { prop: 'sourceType', label: '数据来源', width: 100, slot: 'sourceType' },
  { prop: 'recordTime', label: '记录时间', width: 170 },
  { prop: 'airTemperature', label: '气温(℃)', width: 100 },
  { prop: 'airHumidity', label: '湿度(%)', width: 100 },
  { prop: 'soilMoisture', label: '土壤湿度(%)', width: 110 },
  { prop: 'rainfall', label: '降雨量(mm)', width: 110 },
  { prop: 'windSpeed', label: '风速(m/s)', width: 100 },
]

const fetchFn = (params: Record<string, unknown>) =>
  environmentRecordApi.getList(params) as Promise<{ data: { records: RiceEnvironmentRecord[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RiceEnvironmentRecord>(fetchFn)

// 批次选项
const batchOptions = ref<{ plantingBatchId: string; riceVariety: string }[]>([])

onMounted(async () => {
  loadData()
  const res = await plantingBatchApi.getList({ pageSize: 100 })
  if (res.code === 200) {
    batchOptions.value = res.data.records.map((b) => ({
      plantingBatchId: b.plantingBatchId,
      riceVariety: b.riceVariety,
    }))
  }
})

// 新增弹窗
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  plantingBatchId: '',
  sourceType: 'MANUAL',
  recordTime: '',
  airTemperature: undefined as number | undefined,
  airHumidity: undefined as number | undefined,
  soilMoisture: undefined as number | undefined,
  rainfall: undefined as number | undefined,
  windSpeed: undefined as number | undefined,
  remark: '',
})

const rules: FormRules = {
  plantingBatchId: [{ required: true, message: '请选择种植批次', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择数据来源', trigger: 'change' }],
  recordTime: [{ required: true, message: '请选择记录时间', trigger: 'change' }],
}

function openCreate() {
  form.plantingBatchId = ''
  form.sourceType = 'MANUAL'
  form.recordTime = ''
  form.airTemperature = undefined
  form.airHumidity = undefined
  form.soilMoisture = undefined
  form.rainfall = undefined
  form.windSpeed = undefined
  form.remark = ''
  showForm.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    // Mock: 直接加到本地数据
    const newRecord: RiceEnvironmentRecord = {
      environmentRecordId: mockId('ENV'),
      tenantId: 'TENANT001',
      plantingBatchId: form.plantingBatchId,
      sourceType: form.sourceType as never,
      recordTime: form.recordTime,
      airTemperature: form.airTemperature,
      airHumidity: form.airHumidity,
      soilMoisture: form.soilMoisture,
      rainfall: form.rainfall,
      windSpeed: form.windSpeed,
      remark: form.remark,
      createdBy: 'ADMIN001',
      createdAt: new Date().toISOString(),
    }
    mockEnvironmentRecords.unshift(newRecord)
    ElMessage.success('环境记录添加成功')
    showForm.value = false
    loadData()
  } catch {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}
</script>
