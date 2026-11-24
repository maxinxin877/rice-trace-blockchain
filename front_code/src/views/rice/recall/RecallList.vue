<template>
  <PageContainer title="召回管理">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 发起召回
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
      <template #recallNo="{ row }">
        <el-link type="primary" @click="goDetail(row)">{{ row.recallNo }}</el-link>
      </template>
      <template #triggerType="{ row }">
        <el-tag size="small" effect="plain">{{ TRIGGER_MAP[row.triggerType] || row.triggerType }}</el-tag>
      </template>
      <template #riskLevel="{ row }">
        <StatusTag type="risk" :value="row.riskLevel" />
      </template>
      <template #status="{ row }">
        <el-tag
          size="small"
          :color="RECALL_STATUS_MAP[row.status]?.color"
          :style="{ color: '#fff', border: 'none' }"
        >
          {{ RECALL_STATUS_MAP[row.status]?.label || row.status }}
        </el-tag>
      </template>
      <template #affected="{ row }">
        <span>{{ row.affectedBatchCount }} 批 / {{ row.affectedCodeCount }} 码</span>
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="goDetail(row)">详情</el-button>
      </template>
    </DataTable>

    <!-- 新建召回 -->
    <el-dialog v-model="createVisible" title="发起召回" width="720px" :close-on-click-modal="false">
      <el-steps :active="impact ? 1 : 0" simple class="recall-steps">
        <el-step title="选择问题范围" />
        <el-step title="确认影响范围" />
        <el-step title="提交审批" />
      </el-steps>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="触发类型" prop="triggerType">
              <el-select v-model="form.triggerType" style="width: 100%">
                <el-option
                  v-for="(label, value) in TRIGGER_MAP"
                  :key="value"
                  :label="label"
                  :value="value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级" prop="riskLevel">
              <el-select v-model="form.riskLevel" style="width: 100%">
                <el-option label="低风险" value="LOW" />
                <el-option label="中风险" value="MEDIUM" />
                <el-option label="高风险" value="HIGH" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="范围类型" prop="scopeBusinessType">
              <el-select v-model="form.scopeBusinessType" style="width: 100%">
                <el-option label="单品码" value="TRACE_CODE" />
                <el-option label="成品批次" value="PRODUCT_BATCH" />
                <el-option label="加工批次" value="MILLING_BATCH" />
                <el-option label="原粮批次" value="GRAIN_BATCH" />
                <el-option label="入库单" value="STORAGE_RECEIPT" />
                <el-option label="种植批次" value="PLANTING_BATCH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="范围ID" prop="scopeBusinessId">
              <el-input v-model="form.scopeBusinessId" placeholder="批次号 / 单品码 / 入库单号">
                <template #append>
                  <el-button :loading="previewing" @click="previewScope">预览范围</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="召回原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="如：抽检发现该批次水分超标，存在霉变风险"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="逐码冻结">
              <el-switch v-model="form.includeTraceCodes" active-text="纳入单个防伪码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最多纳入码数">
              <el-input-number
                v-model="form.maxTraceCodeItems"
                :min="0"
                :max="2000"
                :step="50"
                :disabled="!form.includeTraceCodes"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="处置截止时间">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <!-- 影响范围预览 -->
      <div v-if="impact" class="scope-preview">
        <h4>影响范围（自动计算）</h4>
        <div class="scope-cards">
          <div class="scope-card">
            <span>成品批次</span>
            <strong>{{ impact.productBatches.length }}</strong>
          </div>
          <div class="scope-card">
            <span>防伪码</span>
            <strong>{{ impact.traceCodeCount }}</strong>
          </div>
          <div class="scope-card">
            <span>已扫码（已售）</span>
            <strong class="danger">{{ impact.scannedCodeCount }}</strong>
          </div>
          <div class="scope-card">
            <span>重量(kg)</span>
            <strong>{{ Number(impact.weightKg || 0).toLocaleString() }}</strong>
          </div>
        </div>
        <el-alert
          v-if="impact.tips.length"
          type="warning"
          :closable="false"
          show-icon
          class="scope-tips"
        >
          <template #default>
            <div v-for="(tip, idx) in impact.tips" :key="idx">· {{ tip }}</div>
          </template>
        </el-alert>
        <el-table :data="impact.productBatches" size="small" border>
          <el-table-column prop="productBatchId" label="成品批次" min-width="150" />
          <el-table-column prop="productName" label="产品" min-width="120" />
          <el-table-column prop="expectedSaleRegion" label="预期销售区域" min-width="140" />
          <el-table-column prop="traceCodeCount" label="防伪码" width="90" align="center" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button :disabled="!impact" :loading="creating" type="primary" @click="submitCreate">
          创建召回任务
        </el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { recallApi } from '@/api/modules/recall'
import { useTable } from '@/composables/useTable'
import type { RecallTask } from '@/types/recall'
import { RECALL_STATUS_MAP, RECALL_TRIGGER_MAP } from '@/types/recall'
import type { TraceImpact } from '@/types/traceQuery'

const router = useRouter()
const route = useRoute()
const TRIGGER_MAP = RECALL_TRIGGER_MAP

const searchItems: SearchItem[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '召回编号 / 原因 / 批次号' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: Object.entries(RECALL_STATUS_MAP).map(([value, item]) => ({ value, label: item.label })),
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
  {
    prop: 'triggerType',
    label: '触发类型',
    type: 'select',
    options: Object.entries(RECALL_TRIGGER_MAP).map(([value, label]) => ({ value, label })),
  },
]

const columns: TableColumn[] = [
  { prop: 'recallNo', label: '召回编号', width: 170, slot: 'recallNo' },
  { prop: 'triggerType', label: '触发类型', width: 110, slot: 'triggerType' },
  { prop: 'riskLevel', label: '等级', width: 90, slot: 'riskLevel' },
  { prop: 'affected', label: '影响范围', width: 150, slot: 'affected' },
  { prop: 'affectedRegions', label: '涉及区域', minWidth: 160 },
  { prop: 'recallReason', label: '召回原因', minWidth: 220 },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'initiatedBy', label: '发起人', width: 110 },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const fetchFn = (params: Record<string, unknown>) =>
  recallApi.getList(params) as Promise<{ data: { records: RecallTask[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } =
  useTable<RecallTask>(fetchFn)

onMounted(async () => {
  await loadData()
  // 从追溯中心带参跳转：自动打开新建弹窗并预计算影响范围
  const scopeType = route.query.scopeType as string | undefined
  const scopeId = route.query.scopeId as string | undefined
  if (scopeType && scopeId) {
    openCreate()
    form.scopeBusinessType = scopeType
    form.scopeBusinessId = scopeId
    previewScope()
  }
})

function goDetail(row: RecallTask) {
  router.push(`/rice/recalls/detail/${row.recallTaskId}`)
}

// ---------------- 新建召回 ----------------
const createVisible = ref(false)
const creating = ref(false)
const previewing = ref(false)
const impact = ref<TraceImpact | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  triggerType: 'QUALITY_FAIL',
  riskLevel: 'MEDIUM',
  scopeBusinessType: 'PRODUCT_BATCH',
  scopeBusinessId: '',
  reason: '',
  includeTraceCodes: true,
  maxTraceCodeItems: 200,
  deadline: undefined as string | undefined,
})

const rules: FormRules = {
  triggerType: [{ required: true, message: '请选择触发类型', trigger: 'change' }],
  scopeBusinessType: [{ required: true, message: '请选择范围类型', trigger: 'change' }],
  scopeBusinessId: [{ required: true, message: '请输入范围ID', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入召回原因', trigger: 'blur' }],
}

function openCreate() {
  impact.value = null
  form.scopeBusinessId = ''
  form.reason = ''
  form.triggerType = 'QUALITY_FAIL'
  form.riskLevel = 'MEDIUM'
  createVisible.value = true
}

async function previewScope() {
  if (!form.scopeBusinessType || !form.scopeBusinessId.trim()) {
    ElMessage.warning('请先选择范围类型并填写范围ID')
    return
  }
  previewing.value = true
  try {
    const res = await recallApi.previewScope(form.scopeBusinessType, form.scopeBusinessId.trim())
    impact.value = res.data
  } catch {
    impact.value = null
  } finally {
    previewing.value = false
  }
}

async function submitCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!impact.value) {
    ElMessage.warning('请先预览影响范围，确认召回会波及的产品数量')
    return
  }
  creating.value = true
  try {
    const res = await recallApi.create({
      triggerType: form.triggerType as never,
      triggerBusinessType: form.scopeBusinessType,
      triggerBusinessId: form.scopeBusinessId.trim(),
      scopeBusinessType: form.scopeBusinessType,
      scopeBusinessId: form.scopeBusinessId.trim(),
      riskLevel: form.riskLevel as never,
      reason: form.reason,
      includeTraceCodes: form.includeTraceCodes,
      maxTraceCodeItems: form.maxTraceCodeItems,
      deadline: form.deadline,
    })
    ElMessage.success('召回任务已创建，请提交审批')
    createVisible.value = false
    const task = res.data
    router.push(`/rice/recalls/detail/${task.recallTaskId}`)
  } catch {
    // 拦截器已提示
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.recall-steps {
  margin-bottom: 18px;
}

.scope-preview {
  border-top: 1px dashed #e5e6eb;
  padding-top: 14px;
  margin-top: 6px;
}

.scope-preview h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.scope-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.scope-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.scope-card span {
  font-size: 12px;
  color: #909399;
}

.scope-card strong {
  font-size: 18px;
  color: #1a5632;
}

.scope-card strong.danger {
  color: #c53030;
}

.scope-tips {
  margin-bottom: 12px;
}
</style>
