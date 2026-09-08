<template>
  <PageContainer title="农事记录">
    <!-- 批次列表视图 -->
    <template v-if="!selectedBatch">
      <el-alert type="info" :closable="false" title="请选择种植批次查看其农事记录" style="margin-bottom: 16px" />
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
          <el-icon><Plus /></el-icon> 新增农事记录
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
        <template #operationType="{ row }">
          <StatusTag type="farming" :value="row.operationType" />
        </template>
        <template #chainStatus="{ row }">
          <StatusTag type="chain" :value="row.chainStatus" />
        </template>
      </DataTable>
    </template>

    <!-- 新增农事记录弹窗（批次固定为当前选中批次） -->
    <el-dialog v-model="showForm" title="新增农事记录" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="农事类型" prop="operationType">
          <el-select v-model="form.operationType" style="width: 100%">
            <el-option
              v-for="(item, value) in FARMING_OPERATION_TYPE_MAP"
              :key="value"
              :label="item.label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间" prop="operationTime">
          <el-date-picker v-model="form.operationTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="操作人" prop="operatorName">
          <el-input v-model="form.operatorName" placeholder="操作人姓名" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="操作描述" />
        </el-form-item>
        <template v-if="form.operationType === 'PESTICIDE'">
          <el-divider content-position="left">投入品信息（用药必填）</el-divider>
          <el-form-item label="投入品名称" prop="materialName">
            <el-input v-model="form.materialName" placeholder="如: 吡虫啉" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用量" prop="materialDosage">
                <el-input-number v-model="form.materialDosage" :min="0.01" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="单位">
                <el-input v-model="form.materialUnit" placeholder="如: kg/亩" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="安全间隔期" prop="safeIntervalDays">
            <el-input-number v-model="form.safeIntervalDays" :min="0" style="width: 100%" />
            <span class="form-hint">单位：天</span>
          </el-form-item>
        </template>
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
import { farmingLogApi } from '@/api/modules/farmingLog'
import { plantingBatchApi } from '@/api/modules/plantingBatch'
import { useTable } from '@/composables/useTable'
import { FARMING_OPERATION_TYPE_MAP } from '@/utils/constants'
import type { RiceFarmingLog } from '@/types/farmingLog'
import type { RicePlantingBatch } from '@/types/plantingBatch'

// 批次选择
const batchOptions = ref<RicePlantingBatch[]>([])
const batchLoading = ref(false)
const selectedBatch = ref<RicePlantingBatch | null>(null)

const columns: TableColumn[] = [
  { prop: 'logId', label: '记录ID', width: 180 },
  { prop: 'operationType', label: '农事类型', width: 90, slot: 'operationType' },
  { prop: 'operationTime', label: '操作时间', width: 170 },
  { prop: 'operatorName', label: '操作人', width: 90 },
  { prop: 'materialName', label: '投入品', width: 120 },
  { prop: 'description', label: '描述', minWidth: 180 },
  { prop: 'chainStatus', label: '链上状态', width: 90, slot: 'chainStatus' },
]

const fetchFn = (params: Record<string, unknown>) =>
  farmingLogApi.getList({ ...params, plantingBatchId: selectedBatch.value?.plantingBatchId }) as Promise<{ data: { records: RiceFarmingLog[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onPageChange } = useTable<RiceFarmingLog>(fetchFn)

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
  operationType: '',
  operationTime: '',
  operatorName: '',
  description: '',
  materialName: '',
  materialDosage: undefined as number | undefined,
  materialUnit: '',
  safeIntervalDays: undefined as number | undefined,
})

const rules: FormRules = {
  operationType: [{ required: true, message: '请选择农事类型', trigger: 'change' }],
  operationTime: [{ required: true, message: '请选择操作时间', trigger: 'change' }],
  operatorName: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
  materialName: [{
    validator: (_r, v, cb) => {
      if (form.operationType === 'PESTICIDE' && !v) return cb(new Error('用药时投入品名称必填'))
      cb()
    }, trigger: 'blur',
  }],
  materialDosage: [{
    validator: (_r, v, cb) => {
      if (form.operationType === 'PESTICIDE' && (!v || v <= 0)) return cb(new Error('用量必须大于0'))
      cb()
    }, trigger: 'blur',
  }],
  safeIntervalDays: [{
    validator: (_r, v, cb) => {
      if (form.operationType === 'PESTICIDE' && (v === undefined || v < 0)) return cb(new Error('安全间隔期必须≥0'))
      cb()
    }, trigger: 'blur',
  }],
}

function openCreate() {
  form.operationType = ''
  form.operationTime = ''
  form.operatorName = ''
  form.description = ''
  form.materialName = ''
  form.materialDosage = undefined
  form.materialUnit = ''
  form.safeIntervalDays = undefined
  showForm.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await farmingLogApi.create({
      plantingBatchId: selectedBatch.value!.plantingBatchId,
      operationType: form.operationType as never,
      operationTime: form.operationTime,
      operatorId: 'FARMER001',
      operatorName: form.operatorName,
      materialName: form.materialName || undefined,
      materialDosage: form.materialDosage,
      materialUnit: form.materialUnit || undefined,
      safeIntervalDays: form.safeIntervalDays,
      description: form.description || undefined,
    })
    ElMessage.success('农事记录添加成功')
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

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
