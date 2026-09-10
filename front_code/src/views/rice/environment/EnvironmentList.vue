<template>
  <PageContainer title="环境数据">
    <!-- 批次列表视图 -->
    <template v-if="!selectedBatch">
      <el-alert type="info" :closable="false" title="请选择种植批次查看其环境数据" style="margin-bottom: 16px" />
      <el-table
        :data="batchOptions"
        v-loading="batchLoading"
        border
        stripe
        @row-click="selectBatch"
        style="cursor: pointer"
      >
        <el-table-column prop="plantingBatchId" label="批次ID" width="200" />
        <el-table-column prop="riceVariety" label="水稻品种" width="160" />
        <el-table-column prop="fieldName" label="地块名称" min-width="160" />
        <el-table-column label="批次状态" width="110">
          <template #default="{ row }">
            <StatusTag type="batch" :value="row.status" />
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 记录视图 -->
    <template v-else>
      <div class="batch-bar">
        <el-button @click="backToBatchList">返回批次列表</el-button>
        <span class="batch-info">
          当前批次：{{ selectedBatch.plantingBatchId }} — {{ selectedBatch.riceVariety }}<template v-if="selectedBatch.fieldName">（{{ selectedBatch.fieldName }}）</template>
        </span>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 新增环境记录
        </el-button>
      </div>

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
      </DataTable>
    </template>

    <!-- 新增环境记录弹窗（批次固定为当前选中批次） -->
    <el-dialog v-model="showForm" title="新增环境记录" width="550px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
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
          <el-date-picker v-model="form.recordTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
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
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { environmentRecordApi } from '@/api/modules/environmentRecord'
import { plantingBatchApi } from '@/api/modules/plantingBatch'
import { useTable } from '@/composables/useTable'
import { ENVIRONMENT_SOURCE_TYPE_MAP } from '@/utils/constants'
import type { RiceEnvironmentRecord } from '@/types/environmentRecord'
import type { RicePlantingBatch } from '@/types/plantingBatch'

// 批次选择
const batchOptions = ref<RicePlantingBatch[]>([])
const batchLoading = ref(false)
const selectedBatch = ref<RicePlantingBatch | null>(null)

const columns: TableColumn[] = [
  { prop: 'environmentRecordId', label: '记录ID', width: 200 },
  { prop: 'sourceType', label: '数据来源', width: 100, slot: 'sourceType' },
  { prop: 'recordTime', label: '记录时间', width: 170 },
  { prop: 'airTemperature', label: '气温(℃)', width: 100 },
  { prop: 'airHumidity', label: '湿度(%)', width: 100 },
  { prop: 'soilMoisture', label: '土壤湿度(%)', width: 110 },
  { prop: 'rainfall', label: '降雨量(mm)', width: 110 },
  { prop: 'windSpeed', label: '风速(m/s)', width: 100 },
]

const fetchFn = (params: Record<string, unknown>) =>
  environmentRecordApi.getList({ ...params, plantingBatchId: selectedBatch.value?.plantingBatchId }) as Promise<{ data: { records: RiceEnvironmentRecord[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onPageChange } = useTable<RiceEnvironmentRecord>(fetchFn)

onMounted(async () => {
  batchLoading.value = true
  try {
    const res = await plantingBatchApi.getList({ pageSize: 100 })
    if (res.code === 200) batchOptions.value = res.data.records
  } finally {
    batchLoading.value = false
  }
})

function selectBatch(row: RicePlantingBatch) {
  selectedBatch.value = row
  loadData()
}

function backToBatchList() {
  selectedBatch.value = null
  data.value = []
  total.value = 0
}

// 新增弹窗
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
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
  sourceType: [{ required: true, message: '请选择数据来源', trigger: 'change' }],
  recordTime: [{ required: true, message: '请选择记录时间', trigger: 'change' }],
}

function openCreate() {
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
    await environmentRecordApi.create(selectedBatch.value!.plantingBatchId, {
      sourceType: form.sourceType,
      recordTime: form.recordTime,
      airTemperature: form.airTemperature,
      airHumidity: form.airHumidity,
      soilMoisture: form.soilMoisture,
      rainfall: form.rainfall,
      windSpeed: form.windSpeed,
      remark: form.remark,
    })
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

<style scoped>
.batch-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.batch-info {
  font-size: 14px;
  color: #4e5969;
  flex: 1;
}
</style>
