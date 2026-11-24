<template>
  <PageContainer title="不合格品处置">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新建处置单
      </el-button>
    </template>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="disposal-tip"
      title="处置单评审通过后自动隔离，隔离期间该批次在入库/加工/赋码/销售环节会被防错规则拦截；关闭处置单即解除隔离。"
    />

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
      <template #disposalNo="{ row }">
        <el-link type="primary" @click="showDetail(row)">{{ row.disposalNo }}</el-link>
      </template>
      <template #defectType="{ row }">
        {{ DEFECT_MAP[row.defectType] || row.defectType }}
      </template>
      <template #disposition="{ row }">
        <el-tag size="small" effect="plain">{{ METHOD_MAP[row.disposition]?.label || row.disposition }}</el-tag>
      </template>
      <template #riskLevel="{ row }">
        <StatusTag type="risk" :value="row.riskLevel" />
      </template>
      <template #isolated="{ row }">
        <el-tag size="small" :type="row.isolated ? 'danger' : 'info'">
          {{ row.isolated ? '已隔离' : '未隔离' }}
        </el-tag>
      </template>
      <template #status="{ row }">
        <el-tag
          size="small"
          :color="STATUS_MAP[row.status]?.color"
          :style="{ color: '#fff', border: 'none' }"
        >
          {{ STATUS_MAP[row.status]?.label || row.status }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button v-if="row.status === 'DRAFT'" type="primary" link size="small" @click="doSubmit(row)">
          提交
        </el-button>
        <el-button
          v-if="row.status === 'PENDING_REVIEW'"
          type="warning"
          link
          size="small"
          @click="openReview(row)"
        >
          评审
        </el-button>
        <el-button
          v-if="row.status === 'APPROVED'"
          type="success"
          link
          size="small"
          @click="openExecute(row)"
        >
          执行
        </el-button>
        <el-button
          v-if="row.status === 'APPROVED' || row.status === 'EXECUTED'"
          type="info"
          link
          size="small"
          @click="openClose(row)"
        >
          关闭
        </el-button>
      </template>
    </DataTable>

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建不合格品处置单" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="问题对象" prop="businessType">
              <el-select v-model="form.businessType" style="width: 100%">
                <el-option
                  v-for="(label, value) in BUSINESS_MAP"
                  :key="value"
                  :label="label"
                  :value="value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象ID" prop="businessId">
              <el-input v-model="form.businessId" placeholder="批次号 / 单品码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="缺陷类型" prop="defectType">
              <el-select v-model="form.defectType" style="width: 100%">
                <el-option v-for="(label, value) in DEFECT_MAP" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel" style="width: 100%">
                <el-option label="低风险" value="LOW" />
                <el-option label="中风险" value="MEDIUM" />
                <el-option label="高风险" value="HIGH" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="问题描述" prop="defectDesc">
          <el-input v-model="form.defectDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="涉及数量">
              <el-input-number v-model="form.quantity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="涉及重量(kg)">
              <el-input-number v-model="form.weightKg" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="处置方式" prop="disposition">
              <el-select v-model="form.disposition" style="width: 100%">
                <el-option
                  v-for="(item, value) in METHOD_MAP"
                  :key="value"
                  :label="item.label"
                  :value="value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          v-if="METHOD_MAP[form.disposition]"
          type="warning"
          :closable="false"
          show-icon
          :title="METHOD_MAP[form.disposition].hint"
        />
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 评审 -->
    <el-dialog v-model="reviewVisible" title="不合格品评审" width="520px">
      <el-form label-width="90px">
        <el-form-item label="评审结论">
          <el-radio-group v-model="reviewApproved">
            <el-radio :value="true">通过（隔离并执行处置）</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评审意见">
          <el-input v-model="reviewComment" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="处置单详情" size="520px">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="处置单号">{{ current.disposalNo }}</el-descriptions-item>
          <el-descriptions-item label="问题对象">
            {{ BUSINESS_MAP[current.businessType] || current.businessType }}：{{ current.businessId }}
          </el-descriptions-item>
          <el-descriptions-item label="批次/产品">{{ current.batchNo }} {{ current.productName }}</el-descriptions-item>
          <el-descriptions-item label="缺陷">{{ DEFECT_MAP[current.defectType] || current.defectType }}</el-descriptions-item>
          <el-descriptions-item label="问题描述">{{ current.defectDesc }}</el-descriptions-item>
          <el-descriptions-item label="处置方式">
            {{ METHOD_MAP[current.disposition]?.label || current.disposition }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ STATUS_MAP[current.status]?.label || current.status }}
            （{{ current.isolated ? '隔离中' : '未隔离' }}）
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ current.applicant }} / {{ current.appliedAt }}</el-descriptions-item>
          <el-descriptions-item label="评审人">{{ current.reviewer }} / {{ current.reviewedAt }}</el-descriptions-item>
          <el-descriptions-item label="评审意见">{{ current.reviewComment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="执行说明">{{ current.executeRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="链上状态">{{ current.chainStatus }}</el-descriptions-item>
          <el-descriptions-item label="交易ID">{{ current.txId || '待上链' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { disposalApi } from '@/api/modules/disposal'
import { useTable } from '@/composables/useTable'
import type { Disposal } from '@/types/disposal'
import {
  DEFECT_TYPE_MAP,
  DISPOSAL_BUSINESS_MAP,
  DISPOSAL_METHOD_MAP,
  DISPOSAL_STATUS_MAP,
} from '@/types/disposal'

const STATUS_MAP = DISPOSAL_STATUS_MAP
const METHOD_MAP = DISPOSAL_METHOD_MAP
const DEFECT_MAP = DEFECT_TYPE_MAP
const BUSINESS_MAP = DISPOSAL_BUSINESS_MAP

const searchItems: SearchItem[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '处置单号/批次/产品' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: Object.entries(DISPOSAL_STATUS_MAP).map(([value, item]) => ({ value, label: item.label })),
  },
  {
    prop: 'disposition',
    label: '处置方式',
    type: 'select',
    options: Object.entries(DISPOSAL_METHOD_MAP).map(([value, item]) => ({ value, label: item.label })),
  },
  {
    prop: 'riskLevel',
    label: '风险等级',
    type: 'select',
    options: [
      { value: 'LOW', label: '低风险' },
      { value: 'MEDIUM', label: '中风险' },
      { value: 'HIGH', label: '高风险' },
    ],
  },
]

const columns: TableColumn[] = [
  { prop: 'disposalNo', label: '处置单号', width: 170, slot: 'disposalNo' },
  { prop: 'batchNo', label: '批次', width: 160 },
  { prop: 'productName', label: '产品', minWidth: 140 },
  { prop: 'defectType', label: '缺陷', width: 120, slot: 'defectType' },
  { prop: 'disposition', label: '处置方式', width: 120, slot: 'disposition' },
  { prop: 'riskLevel', label: '等级', width: 90, slot: 'riskLevel' },
  { prop: 'isolated', label: '隔离', width: 90, slot: 'isolated' },
  { prop: 'status', label: '状态', width: 130, slot: 'status' },
  { prop: 'applicant', label: '申请人', width: 110 },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const fetchFn = (params: Record<string, unknown>) =>
  disposalApi.getList(params) as Promise<{ data: { records: Disposal[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } =
  useTable<Disposal>(fetchFn)

onMounted(() => loadData())

// ---------------- 新建 ----------------
const createVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  businessType: 'PRODUCT_BATCH',
  businessId: '',
  defectType: 'MOISTURE',
  defectDesc: '',
  riskLevel: 'MEDIUM',
  quantity: 0,
  weightKg: 0,
  disposition: 'ISOLATE',
})

const rules: FormRules = {
  businessType: [{ required: true, message: '请选择问题对象类型', trigger: 'change' }],
  businessId: [{ required: true, message: '请输入对象ID', trigger: 'blur' }],
  defectType: [{ required: true, message: '请选择缺陷类型', trigger: 'change' }],
  defectDesc: [{ required: true, message: '请填写问题描述', trigger: 'blur' }],
  disposition: [{ required: true, message: '请选择处置方式', trigger: 'change' }],
}

function openCreate() {
  form.businessId = ''
  form.defectDesc = ''
  createVisible.value = true
}

async function submitCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await disposalApi.create({
      businessType: form.businessType,
      businessId: form.businessId.trim(),
      defectType: form.defectType,
      defectDesc: form.defectDesc,
      riskLevel: form.riskLevel as never,
      quantity: form.quantity,
      weightKg: form.weightKg,
      disposition: form.disposition,
    })
    ElMessage.success('处置单已创建，请提交评审')
    createVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function doSubmit(row: Disposal) {
  await disposalApi.submit(row.disposalId)
  ElMessage.success('已提交评审')
  loadData()
}

// ---------------- 评审 ----------------
const reviewVisible = ref(false)
const reviewApproved = ref(true)
const reviewComment = ref('')
const currentId = ref('')

function openReview(row: Disposal) {
  currentId.value = row.disposalId
  reviewApproved.value = true
  reviewComment.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  submitting.value = true
  try {
    await disposalApi.review(currentId.value, reviewApproved.value, reviewComment.value)
    ElMessage.success(reviewApproved.value ? '评审通过，已隔离' : '已驳回')
    reviewVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---------------- 执行 / 关闭 ----------------
async function openExecute(row: Disposal) {
  try {
    const input = (await ElMessageBox.prompt(
      `处置方式：${METHOD_MAP[row.disposition]?.label || row.disposition}，请填写执行说明`,
      '执行处置',
      { inputType: 'textarea', inputPlaceholder: '如：2026-11-03 完成返工并重新检验合格' },
    )) as { value: string }
    await disposalApi.execute(row.disposalId, input.value || '')
    ElMessage.success('处置已执行')
    loadData()
  } catch {
    // 取消
  }
}

async function openClose(row: Disposal) {
  try {
    const input = (await ElMessageBox.prompt('关闭后解除隔离，请填写归档说明', '关闭处置单', {
      inputType: 'textarea',
    })) as { value: string }
    await disposalApi.close(row.disposalId, input.value || '')
    ElMessage.success('已关闭并解除隔离')
    loadData()
  } catch {
    // 取消
  }
}

// ---------------- 详情 ----------------
const detailVisible = ref(false)
const current = ref<Disposal | null>(null)

async function showDetail(row: Disposal) {
  const res = await disposalApi.getById(row.disposalId)
  current.value = res.data
  detailVisible.value = true
}
</script>

<style scoped>
.disposal-tip {
  margin-bottom: 14px;
}
</style>
