<template>
  <PageContainer title="种植批次管理">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增种植批次
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
        <StatusTag type="batch" :value="row.status" />
      </template>
      <template #chainStatus="{ row }">
        <StatusTag type="chain" :value="row.chainStatus" />
      </template>
      <template #organicCertified="{ row }">
        {{ row.organicCertified ? '✅' : '-' }}
      </template>
      <template #greenCertified="{ row }">
        {{ row.greenCertified ? '✅' : '-' }}
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="viewDetail(row.plantingBatchId)">详情</el-button>
      </template>
    </DataTable>

    <!-- 新增种植批次弹窗 -->
    <el-dialog v-model="showForm" title="新增种植批次" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="选择地块" prop="fieldId">
          <el-select v-model="form.fieldId" placeholder="请选择地块" filterable style="width: 100%">
            <el-option
              v-for="f in fieldOptions"
              :key="f.fieldId"
              :label="`${f.fieldName} (${f.fieldCode})`"
              :value="f.fieldId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="水稻品种" prop="riceVariety">
          <el-input v-model="form.riceVariety" placeholder="如: 五优稻4号" />
        </el-form-item>
        <el-form-item label="种子来源" prop="seedSource">
          <el-input v-model="form.seedSource" placeholder="如: 五常市种子公司" />
        </el-form-item>
        <el-form-item label="种子批号">
          <el-input v-model="form.seedBatchNo" placeholder="种子批号" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="播种日期" prop="sowingDate">
              <el-date-picker v-model="form.sowingDate" type="date" value-format="YYYY-MM-DD" placeholder="播种日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计收割日期">
              <el-date-picker v-model="form.expectedHarvestDate" type="date" value-format="YYYY-MM-DD" placeholder="预计收割日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="有机认证">
              <el-switch v-model="form.organicCertified" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绿色认证">
              <el-switch v-model="form.greenCertified" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认创建</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { plantingBatchApi } from '@/api/modules/plantingBatch'
import { fieldApi } from '@/api/modules/field'
import { useTable } from '@/composables/useTable'
import { RICE_BATCH_STATUS_MAP } from '@/utils/constants'
import type { RicePlantingBatch } from '@/types/plantingBatch'

const router = useRouter()

const searchItems: SearchItem[] = [
  { prop: 'riceVariety', label: '水稻品种', type: 'input', placeholder: '输入品种名称' },
  { prop: 'status', label: '批次状态', type: 'select', options: Object.entries(RICE_BATCH_STATUS_MAP).map(([value, item]) => ({ value, label: item.label })) },
]

const columns: TableColumn[] = [
  { prop: 'plantingBatchId', label: '批次ID', width: 180 },
  { prop: 'fieldName', label: '地块名称', width: 150 },
  { prop: 'riceVariety', label: '水稻品种', width: 140 },
  { prop: 'seedSource', label: '种子来源', width: 180 },
  { prop: 'sowingDate', label: '播种日期', width: 120 },
  { prop: 'status', label: '批次状态', width: 90, slot: 'status' },
  { prop: 'organicCertified', label: '有机认证', width: 85, slot: 'organicCertified' },
  { prop: 'greenCertified', label: '绿色认证', width: 85, slot: 'greenCertified' },
  { prop: 'chainStatus', label: '链上状态', width: 90, slot: 'chainStatus' },
]

const fetchFn = (params: Record<string, unknown>) =>
  plantingBatchApi.getList(params) as Promise<{ data: { records: RicePlantingBatch[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RicePlantingBatch>(fetchFn)

// 地块选项
const fieldOptions = ref<{ fieldId: string; fieldName: string; fieldCode: string }[]>([])

onMounted(async () => {
  loadData()
  // 加载地块列表供选择
  const res = await fieldApi.getList({ pageSize: 100 })
  if (res.code === 200) {
    fieldOptions.value = res.data.records.map((f) => ({
      fieldId: f.fieldId,
      fieldName: f.fieldName,
      fieldCode: f.fieldCode,
    }))
  }
})

function viewDetail(id: string) {
  router.push(`/rice/planting-batches/detail/${id}`)
}

// 新增弹窗
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  fieldId: '',
  riceVariety: '',
  seedSource: '',
  seedBatchNo: '',
  sowingDate: '',
  expectedHarvestDate: '',
  organicCertified: false,
  greenCertified: false,
})

const rules: FormRules = {
  fieldId: [{ required: true, message: '请选择地块', trigger: 'change' }],
  riceVariety: [{ required: true, message: '请输入水稻品种', trigger: 'blur' }],
  seedSource: [{ required: true, message: '请输入种子来源', trigger: 'blur' }],
  sowingDate: [{ required: true, message: '请选择播种日期', trigger: 'change' }],
}

function openCreate() {
  form.fieldId = ''
  form.riceVariety = ''
  form.seedSource = ''
  form.seedBatchNo = ''
  form.sowingDate = ''
  form.expectedHarvestDate = ''
  form.organicCertified = false
  form.greenCertified = false
  showForm.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await plantingBatchApi.create({ ...form })
    ElMessage.success('种植批次创建成功')
    showForm.value = false
    loadData()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}
</script>
