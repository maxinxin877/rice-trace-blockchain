<template>
  <PageContainer title="农事记录">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增农事记录
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
      <template #operationType="{ row }">
        <StatusTag type="farming" :value="row.operationType" />
      </template>
      <template #chainStatus="{ row }">
        <StatusTag type="chain" :value="row.chainStatus" />
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="$router.push(`/rice/planting-batches/detail/${row.plantingBatchId}`)">
          查看批次
        </el-button>
      </template>
    </DataTable>

    <!-- 新增农事记录弹窗 -->
    <el-dialog v-model="showForm" title="新增农事记录" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="关联批次" prop="plantingBatchId">
          <el-select v-model="form.plantingBatchId" placeholder="请选择种植批次" filterable style="width: 100%">
            <el-option
              v-for="b in batchOptions"
              :key="b.plantingBatchId"
              :label="`${b.plantingBatchId} — ${b.riceVariety} (${b.fieldName})`"
              :value="b.plantingBatchId"
            />
          </el-select>
        </el-form-item>
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
          <el-date-picker v-model="form.operationTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
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
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { farmingLogApi } from '@/api/modules/farmingLog'
import { plantingBatchApi } from '@/api/modules/plantingBatch'
import { useTable } from '@/composables/useTable'
import { FARMING_OPERATION_TYPE_MAP } from '@/utils/constants'
import type { RiceFarmingLog } from '@/types/farmingLog'

const searchItems: SearchItem[] = [
  { prop: 'plantingBatchId', label: '种植批次', type: 'input', placeholder: '输入批次ID' },
  { prop: 'operationType', label: '农事类型', type: 'select', options: Object.entries(FARMING_OPERATION_TYPE_MAP).map(([value, item]) => ({ value, label: item.label })) },
]

const columns: TableColumn[] = [
  { prop: 'logId', label: '记录ID', width: 180 },
  { prop: 'plantingBatchId', label: '关联批次', width: 180 },
  { prop: 'operationType', label: '农事类型', width: 90, slot: 'operationType' },
  { prop: 'operationTime', label: '操作时间', width: 170 },
  { prop: 'operatorName', label: '操作人', width: 90 },
  { prop: 'materialName', label: '投入品', width: 120 },
  { prop: 'description', label: '描述', minWidth: 180 },
  { prop: 'chainStatus', label: '链上状态', width: 90, slot: 'chainStatus' },
]

const fetchFn = (params: Record<string, unknown>) =>
  farmingLogApi.getList(params) as Promise<{ data: { records: RiceFarmingLog[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RiceFarmingLog>(fetchFn)

// 批次选项
const batchOptions = ref<{ plantingBatchId: string; riceVariety: string; fieldName: string }[]>([])

onMounted(async () => {
  loadData()
  const res = await plantingBatchApi.getList({ pageSize: 100 })
  if (res.code === 200) {
    batchOptions.value = res.data.records.map((b) => ({
      plantingBatchId: b.plantingBatchId,
      riceVariety: b.riceVariety,
      fieldName: b.fieldName || '',
    }))
  }
})

// 新增弹窗
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  plantingBatchId: '',
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
  plantingBatchId: [{ required: true, message: '请选择种植批次', trigger: 'change' }],
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
  form.plantingBatchId = ''
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
      plantingBatchId: form.plantingBatchId,
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
.form-hint {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
