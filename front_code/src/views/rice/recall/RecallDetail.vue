<template>
  <PageContainer title="召回详情">
    <template #actions>
      <el-button @click="goBack">返回列表</el-button>
      <el-button v-if="task?.status === 'DRAFT'" type="primary" @click="doSubmit">提交审批</el-button>
      <template v-if="task?.status === 'SUBMITTED'">
        <el-button type="success" @click="openComment('approve')">审批通过</el-button>
        <el-button type="danger" plain @click="openComment('reject')">驳回</el-button>
      </template>
      <el-button v-if="task?.status === 'APPROVED'" type="danger" @click="openRelease">发布召回</el-button>
      <el-button
        v-if="task && ['RELEASED', 'HANDLING'].includes(task.status)"
        type="primary"
        @click="openClose"
      >
        关闭召回
      </el-button>
      <el-button
        v-if="task && ['DRAFT', 'SUBMITTED', 'APPROVED'].includes(task.status)"
        @click="doCancel"
      >
        取消
      </el-button>
    </template>

    <div v-loading="loading">
      <template v-if="task">
        <!-- 头部 -->
        <div class="recall-header">
          <div>
            <span class="recall-no">{{ task.recallNo }}</span>
            <el-tag
              size="small"
              :color="RECALL_STATUS_MAP[task.status]?.color"
              :style="{ color: '#fff', border: 'none' }"
            >
              {{ RECALL_STATUS_MAP[task.status]?.label || task.status }}
            </el-tag>
            <StatusTag type="risk" :value="task.riskLevel" />
            <el-tag size="small" effect="plain">{{ TRIGGER_MAP[task.triggerType] || task.triggerType }}</el-tag>
          </div>
          <div class="recall-chain">
            <span v-if="task.txId">链上交易 {{ shortHash(task.txId) }}</span>
            <StatusTag type="chain" :value="task.chainStatus" />
          </div>
        </div>

        <el-steps :active="stepActive" simple class="recall-steps">
          <el-step v-for="step in RECALL_STEPS" :key="step" :title="step" />
        </el-steps>

        <el-alert
          v-if="task.status === 'REJECTED' || task.status === 'CANCELLED'"
          type="info"
          :closable="false"
          show-icon
          class="recall-alert"
          :title="task.status === 'REJECTED' ? '审批已驳回' : '召回已取消'"
          :description="task.status === 'REJECTED' ? task.approveComment : task.closeResult"
        />

        <!-- 基本信息 -->
        <el-descriptions :column="3" border class="recall-desc">
          <el-descriptions-item label="召回原因" :span="3">{{ task.recallReason }}</el-descriptions-item>
          <el-descriptions-item label="影响成品批次">{{ task.affectedBatchCount }}</el-descriptions-item>
          <el-descriptions-item label="影响防伪码">{{ task.affectedCodeCount }}</el-descriptions-item>
          <el-descriptions-item label="影响重量(kg)">{{ formatNumber(task.affectedWeightKg) }}</el-descriptions-item>
          <el-descriptions-item label="涉及区域" :span="3">{{ task.affectedRegions || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ task.initiatedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ task.initiatedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处置截止">{{ task.deadline || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ task.approvedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ task.approvedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批意见">{{ task.approveComment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布冻结">{{ task.releasedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ task.closedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关闭结论">{{ task.closeResult || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 处置进度 -->
        <h4 class="recall-section-title">处置进度（需求 §九 JK-06 召回进度监控）</h4>
        <div class="progress-grid" v-if="task.progress">
          <div class="progress-card">
            <span>通知送达率</span>
            <strong>{{ task.progress.notifyRate }}%</strong>
            <em>{{ task.progress.notifiedCount }}/{{ task.progress.total }}</em>
          </div>
          <div class="progress-card">
            <span>回收完成率</span>
            <strong>{{ task.progress.recoveryRate }}%</strong>
            <em>已处置 {{ task.progress.recoveredCount + task.progress.isolatedCount + task.progress.returnedCount + task.progress.scrappedCount }} 条</em>
          </div>
          <div class="progress-card">
            <span>已下架</span>
            <strong>{{ task.progress.offShelfCount }}</strong>
            <em>终端停止销售</em>
          </div>
          <div class="progress-card">
            <span>未召回</span>
            <strong class="danger">{{ task.progress.unRecoveredCount }}</strong>
            <em>需继续跟踪</em>
          </div>
        </div>
        <el-progress
          v-if="task.progress"
          :percentage="task.progress.recoveryRate"
          :stroke-width="10"
          :status="task.progress.recoveryRate === 100 ? 'success' : undefined"
          class="recall-progress-bar"
        />

        <!-- 召回明细 -->
        <h4 class="recall-section-title">
          召回明细
          <span class="sub">（{{ task.items?.length || 0 }} 条，含成品批次 / 防伪码 / 原粮批次）</span>
        </h4>
        <el-table :data="task.items || []" border stripe max-height="480">
          <el-table-column prop="itemType" label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ ITEM_TYPE_MAP[row.itemType] || row.itemType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="businessId" label="业务ID" min-width="160" show-overflow-tooltip />
          <el-table-column prop="productName" label="产品" min-width="120" />
          <el-table-column prop="packageSpec" label="规格" width="90" align="center" />
          <el-table-column prop="expectedRegion" label="预期区域" min-width="120" />
          <el-table-column prop="actualRegion" label="实际区域" min-width="120" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="冻结" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.frozen ? 'danger' : 'info'">
                {{ row.frozen ? '已冻结' : '未冻结' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="notifyStatus" label="通知" width="90" align="center" />
          <el-table-column label="处置状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag
                size="small"
                :color="RECALL_HANDLE_MAP[row.handleStatus]?.color"
                :style="{ color: '#fff', border: 'none' }"
              >
                {{ RECALL_HANDLE_MAP[row.handleStatus]?.label || row.handleStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handleRemark" label="处置说明" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="task && ['RELEASED', 'HANDLING'].includes(task.status)"
                type="primary"
                link
                size="small"
                @click="openHandle(row)"
              >
                登记处置
              </el-button>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 链上存证 -->
        <h4 class="recall-section-title">链上存证（召回指令不可抵赖）</h4>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="链上状态">{{ task.chainStatus }}</el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ task.blockHeight || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易ID" :span="2">{{ task.txId || '待上链' }}</el-descriptions-item>
          <el-descriptions-item label="上链时间">{{ task.chainTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="异常信息">{{ task.chainError || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </div>

    <!-- 审批 / 驳回 -->
    <el-dialog v-model="commentVisible" :title="commentAction === 'approve' ? '审批通过' : '驳回召回'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="审批意见">
          <el-input v-model="comment" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commentVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doComment">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发布召回 -->
    <el-dialog v-model="releaseVisible" title="发布召回" width="520px">
      <el-alert
        type="error"
        :closable="false"
        show-icon
        title="发布会立即冻结相关批次与防伪码"
        description="冻结后：批次禁止出入库与加工、防伪码置为 DISABLED 并禁止销售，同时召回指令上链、生成监管预警。"
        class="recall-alert"
      />
      <el-form label-width="100px">
        <el-form-item label="通知渠道">
          <el-checkbox-group v-model="channels">
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="WECHAT">微信</el-checkbox>
            <el-checkbox label="STORE_APP">门店APP</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="处置截止">
          <el-date-picker v-model="releaseDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="releaseVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="doRelease">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- 关闭召回 -->
    <el-dialog v-model="closeVisible" title="关闭召回" width="520px">
      <el-form label-width="90px">
        <el-form-item label="关闭结论">
          <el-input
            v-model="closeResult"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="如：涉及产品全部回收销毁，未发现流入市场"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doClose">确认关闭</el-button>
      </template>
    </el-dialog>

    <!-- 明细处置 -->
    <el-dialog v-model="handleVisible" title="登记处置结果" width="520px">
      <el-form label-width="100px">
        <el-form-item label="业务ID">
          <el-input :model-value="currentItem?.businessId" disabled />
        </el-form-item>
        <el-form-item label="处置状态">
          <el-select v-model="handleStatus" style="width: 100%">
            <el-option
              v-for="(item, value) in RECALL_HANDLE_MAP"
              :key="value"
              :label="item.label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处置说明">
          <el-input v-model="handleRemark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doHandle">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { recallApi } from '@/api/modules/recall'
import type { RecallItem, RecallTask } from '@/types/recall'
import {
  RECALL_HANDLE_MAP,
  RECALL_ITEM_TYPE_MAP,
  RECALL_STATUS_MAP,
  RECALL_STEPS,
  RECALL_TRIGGER_MAP,
} from '@/types/recall'

const route = useRoute()
const router = useRouter()
const TRIGGER_MAP = RECALL_TRIGGER_MAP
const ITEM_TYPE_MAP = RECALL_ITEM_TYPE_MAP

const recallTaskId = route.params.id as string
const loading = ref(false)
const submitting = ref(false)
const task = ref<RecallTask | null>(null)

const STEP_INDEX: Record<string, number> = {
  DRAFT: 0,
  SUBMITTED: 1,
  APPROVED: 2,
  RELEASED: 3,
  HANDLING: 4,
  CLOSED: 5,
  REJECTED: 1,
  CANCELLED: 1,
}

const stepActive = computed(() => (task.value ? STEP_INDEX[task.value.status] ?? 0 : 0))

function formatNumber(value: unknown): string {
  const num = Number(value)
  return Number.isNaN(num) ? '-' : num.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function shortHash(hash: string): string {
  return hash.length > 18 ? `${hash.slice(0, 10)}...${hash.slice(-6)}` : hash
}

async function load() {
  loading.value = true
  try {
    const res = await recallApi.getById(recallTaskId)
    task.value = res.data
  } catch {
    task.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)

function goBack() {
  router.push('/rice/recalls')
}

async function doSubmit() {
  submitting.value = true
  try {
    await recallApi.submit(recallTaskId)
    ElMessage.success('已提交审批')
    load()
  } finally {
    submitting.value = false
  }
}

// ---------------- 审批 / 驳回 ----------------
const commentVisible = ref(false)
const commentAction = ref<'approve' | 'reject'>('approve')
const comment = ref('')

function openComment(action: 'approve' | 'reject') {
  commentAction.value = action
  comment.value = ''
  commentVisible.value = true
}

async function doComment() {
  submitting.value = true
  try {
    if (commentAction.value === 'approve') {
      await recallApi.approve(recallTaskId, comment.value)
      ElMessage.success('审批通过，可发布召回')
    } else {
      await recallApi.reject(recallTaskId, comment.value)
      ElMessage.success('已驳回')
    }
    commentVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

// ---------------- 发布 ----------------
const releaseVisible = ref(false)
const channels = ref<string[]>(['SMS', 'WECHAT'])
const releaseDeadline = ref<string | undefined>(undefined)

function openRelease() {
  releaseVisible.value = true
}

async function doRelease() {
  submitting.value = true
  try {
    await recallApi.release(recallTaskId, channels.value)
    ElMessage.success('召回已发布，相关批次与防伪码已冻结')
    releaseVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

// ---------------- 关闭 ----------------
const closeVisible = ref(false)
const closeResult = ref('')

function openClose() {
  closeResult.value = ''
  closeVisible.value = true
}

async function doClose() {
  if (!closeResult.value.trim()) {
    ElMessage.warning('请填写关闭结论')
    return
  }
  submitting.value = true
  try {
    await recallApi.close(recallTaskId, closeResult.value)
    ElMessage.success('召回已关闭')
    closeVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function doCancel() {
  try {
    await ElMessageBox.confirm('取消后本次召回流程终止，已冻结的批次不会自动解冻，确认取消？', '取消召回', {
      type: 'warning',
    })
  } catch {
    return
  }
  await recallApi.cancel(recallTaskId, '发起方取消')
  ElMessage.success('已取消')
  load()
}

// ---------------- 明细处置 ----------------
const handleVisible = ref(false)
const currentItem = ref<RecallItem | null>(null)
const handleStatus = ref('OFF_SHELF')
const handleRemark = ref('')

function openHandle(item: RecallItem) {
  currentItem.value = item
  handleStatus.value = item.handleStatus === 'PENDING' ? 'OFF_SHELF' : item.handleStatus
  handleRemark.value = item.handleRemark || ''
  handleVisible.value = true
}

async function doHandle() {
  if (!currentItem.value) return
  submitting.value = true
  try {
    await recallApi.handleItem(currentItem.value.itemId, handleStatus.value, handleRemark.value)
    ElMessage.success('处置结果已登记')
    handleVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.recall-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 14px;
}

.recall-header > div:first-child {
  display: flex;
  align-items: center;
  gap: 10px;
}

.recall-no {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
}

.recall-chain {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #6b7280;
}

.recall-steps {
  margin-bottom: 16px;
}

.recall-alert {
  margin-bottom: 14px;
}

.recall-desc {
  margin-bottom: 8px;
}

.recall-section-title {
  margin: 20px 0 10px;
  font-size: 14px;
  color: #1f2937;
}

.recall-section-title .sub {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.progress-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.progress-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-card span {
  font-size: 12px;
  color: #909399;
}

.progress-card strong {
  font-size: 20px;
  color: #1a5632;
}

.progress-card strong.danger {
  color: #c53030;
}

.progress-card em {
  font-size: 12px;
  font-style: normal;
  color: #909399;
}

.recall-progress-bar {
  margin-top: 14px;
}

.text-muted {
  color: #c0c4cc;
}
</style>
