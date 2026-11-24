<template>
  <PageContainer title="扫码采集">
    <template #actions>
      <el-tag type="info" effect="plain">需求 §九 CJ-01~CJ-07</el-tag>
    </template>

    <!-- 概览 -->
    <div class="collect-stats">
      <div class="stat-card">
        <span>今日采集</span>
        <strong>{{ stats.todayTotal }}</strong>
        <em>扫码枪 / PDA / 网页</em>
      </div>
      <div class="stat-card">
        <span>今日拦截</span>
        <strong class="danger">{{ stats.todayBlocked }}</strong>
        <em>命中防错规则</em>
      </div>
      <div class="stat-card">
        <span>待审核补录</span>
        <strong class="warning">{{ stats.pendingReview }}</strong>
        <em>人工补录需审核</em>
      </div>
      <div class="stat-card">
        <span>离线待补传</span>
        <strong>{{ offline ? queue.length : stats.offlineTotal }}</strong>
        <em>{{ offline ? '本地队列' : '历史离线记录' }}</em>
      </div>
    </div>

    <div class="collect-layout">
      <!-- 扫码台 -->
      <div class="scan-panel">
        <div class="panel-title">扫码台</div>
        <el-form label-position="top" size="default">
          <el-form-item label="业务动作">
            <el-select v-model="action" style="width: 100%">
              <el-option
                v-for="(label, value) in ACTION_MAP"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="终端类型">
            <el-radio-group v-model="terminalType">
              <el-radio-button value="SCANNER">扫码枪</el-radio-button>
              <el-radio-button value="PDA">PDA</el-radio-button>
              <el-radio-button value="MOBILE">移动端</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="离线模式（弱网演示）">
            <el-switch v-model="offline" active-text="断网缓存，恢复后补传" />
          </el-form-item>
        </el-form>

        <el-input
          ref="scanInputRef"
          v-model="scanCode"
          size="large"
          placeholder="扫描或输入码后回车"
          clearable
          @keyup.enter="submitScan"
        >
          <template #prefix>
            <el-icon><Aim /></el-icon>
          </template>
          <template #append>
            <el-button :loading="submitting" @click="submitScan">采集</el-button>
          </template>
        </el-input>

        <div v-if="offline" class="offline-bar">
          <span>离线队列 {{ queue.length }} 条</span>
          <el-button size="small" type="primary" :loading="uploading" :disabled="!queue.length" @click="flushQueue">
            恢复网络并补传
          </el-button>
        </div>
        <div v-else class="scan-tip">
          提示：扫码后系统自动识别码类型（单品/箱/托盘/批次），并带出业务数据与红绿灯
        </div>
        <div class="manual-entry">
          <el-button link type="primary" @click="manualVisible = true">自动采集失败？人工补录（需审核）</el-button>
        </div>

        <!-- 最近结果 -->
        <div class="recent-title">最近扫描</div>
        <div v-if="recent.length === 0" class="recent-empty">暂无扫描记录</div>
        <div
          v-for="item in recent"
          :key="item.key"
          class="recent-item"
          :style="{ borderLeftColor: RESULT_MAP[item.result]?.color }"
        >
          <div class="recent-head">
            <span class="recent-code">{{ item.code }}</span>
            <el-tag
              size="small"
              :color="RESULT_MAP[item.result]?.color"
              :style="{ color: '#fff', border: 'none' }"
            >
              {{ RESULT_MAP[item.result]?.label }}
            </el-tag>
          </div>
          <div class="recent-meta">
            {{ ACTION_MAP[item.action] || item.action }} ·
            {{ CODE_TYPE_MAP[item.codeType] || item.codeType }} ·
            {{ item.time }}
          </div>
          <div v-if="item.message" class="recent-msg">{{ item.message }}</div>
        </div>
      </div>

      <!-- 采集记录 -->
      <div class="record-panel">
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
          <template #checkResult="{ row }">
            <el-tag
              size="small"
              :color="RESULT_MAP[row.checkResult]?.color"
              :style="{ color: '#fff', border: 'none' }"
            >
              {{ RESULT_MAP[row.checkResult]?.label || row.checkResult }}
            </el-tag>
          </template>
          <template #codeType="{ row }">
            <span>{{ CODE_TYPE_MAP[row.codeType] || row.codeType }}</span>
          </template>
          <template #action="{ row }">
            <span>{{ ACTION_MAP[row.action] || row.action }}</span>
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
          <template #source="{ row }">
            <el-tag size="small" effect="plain" :type="row.source === 'MANUAL' ? 'warning' : row.source === 'OFFLINE' ? 'info' : 'success'">
              {{ row.source === 'MANUAL' ? '人工补录' : row.source === 'OFFLINE' ? '离线补传' : '在线' }}
            </el-tag>
          </template>
          <template #actions="{ row }">
            <template v-if="row.status === 'PENDING_REVIEW'">
              <el-button type="success" link size="small" @click="review(row, true)">通过</el-button>
              <el-button type="danger" link size="small" @click="review(row, false)">驳回</el-button>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </DataTable>
      </div>
    </div>

    <!-- 人工补录 -->
    <el-dialog v-model="manualVisible" title="人工补录（需审核）" width="520px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="自动采集失败时才允许补录"
        description="补录记录会强制填写原因，并进入待审核状态，审核通过后才视为有效采集。"
        class="manual-alert"
      />
      <el-form label-width="90px">
        <el-form-item label="码">
          <el-input v-model="manualForm.code" placeholder="单品码 / 批次号" />
        </el-form-item>
        <el-form-item label="业务动作">
          <el-select v-model="manualForm.action" style="width: 100%">
            <el-option v-for="(label, value) in ACTION_MAP" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="补录原因">
          <el-input v-model="manualForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitManual">提交补录</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import { collectApi } from '@/api/modules/collect'
import { useTable } from '@/composables/useTable'
import type { CollectPayload, CollectRecord, CollectStats } from '@/types/collect'
import {
  COLLECT_ACTION_MAP,
  COLLECT_CODE_TYPE_MAP,
  COLLECT_RESULT_MAP,
  COLLECT_STATUS_MAP,
} from '@/types/collect'

const ACTION_MAP = COLLECT_ACTION_MAP
const RESULT_MAP = COLLECT_RESULT_MAP
const STATUS_MAP = COLLECT_STATUS_MAP
const CODE_TYPE_MAP = COLLECT_CODE_TYPE_MAP

const searchItems: SearchItem[] = [
  { prop: 'code', label: '码值', type: 'input', placeholder: '单品码/批次号' },
  {
    prop: 'action',
    label: '动作',
    type: 'select',
    options: Object.entries(COLLECT_ACTION_MAP).map(([value, label]) => ({ value, label })),
  },
  {
    prop: 'checkResult',
    label: '校验',
    type: 'select',
    options: [
      { value: 'PASS', label: '通过' },
      { value: 'WARN', label: '告警' },
      { value: 'BLOCK', label: '拦截' },
    ],
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: Object.entries(COLLECT_STATUS_MAP).map(([value, item]) => ({ value, label: item.label })),
  },
  {
    prop: 'source',
    label: '来源',
    type: 'select',
    options: [
      { value: 'ONLINE', label: '在线' },
      { value: 'OFFLINE', label: '离线补传' },
      { value: 'MANUAL', label: '人工补录' },
    ],
  },
]

const columns: TableColumn[] = [
  { prop: 'collectTime', label: '采集时间', width: 170 },
  { prop: 'collectCode', label: '码值', minWidth: 170 },
  { prop: 'codeType', label: '码类型', width: 110, slot: 'codeType' },
  { prop: 'action', label: '动作', width: 100, slot: 'action' },
  { prop: 'checkResult', label: '校验', width: 90, slot: 'checkResult' },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'source', label: '来源', width: 100, slot: 'source' },
  { prop: 'terminalType', label: '终端', width: 90 },
  { prop: 'operatorName', label: '操作人', width: 110 },
  { prop: 'checkMessage', label: '校验说明', minWidth: 220 },
]

const fetchFn = (params: Record<string, unknown>) =>
  collectApi.getList(params) as Promise<{ data: { records: CollectRecord[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } =
  useTable<CollectRecord>(fetchFn)

// ---------------- 扫码台 ----------------
const scanInputRef = ref()
const scanCode = ref('')
const action = ref('STORE')
const terminalType = ref('SCANNER')
const submitting = ref(false)
const uploading = ref(false)
const offline = ref(false)
const queue = ref<CollectPayload[]>([])

const recent = ref<{ key: string; code: string; action: string; codeType: string; result: string; message: string; time: string }[]>([])

const stats = ref<CollectStats>({
  todayTotal: 0,
  todayBlocked: 0,
  pendingReview: 0,
  offlineTotal: 0,
  actionDistribution: [],
})

async function loadStats() {
  try {
    const res = await collectApi.stats()
    stats.value = res.data
  } catch {
    // 统计失败不影响主流程
  }
}

onMounted(async () => {
  await loadData()
  await loadStats()
  nextTick(() => scanInputRef.value?.focus?.())
})

function buildKey(code: string, act: string, time: number) {
  return `${act}:${code}:${time}`
}

async function submitScan() {
  const code = scanCode.value.trim()
  if (!code) {
    ElMessage.warning('请扫描或输入码值')
    return
  }
  const payload: CollectPayload = {
    code,
    action: action.value as CollectPayload['action'],
    terminalType: terminalType.value,
    source: offline.value ? 'OFFLINE' : 'ONLINE',
    collectTime: formatNow(),
    idempotencyKey: buildKey(code, action.value, Date.now()),
  }

  if (offline.value) {
    // CJ-06：断网时先入本地队列，保留同一幂等键，恢复后补传不会重复入库
    queue.value.push(payload)
    pushRecent({
      code,
      action: action.value,
      codeType: '待识别',
      result: 'WARN',
      message: '离线缓存中，恢复网络后自动补传',
      time: payload.collectTime as string,
    })
    scanCode.value = ''
    nextTick(() => scanInputRef.value?.focus?.())
    return
  }

  submitting.value = true
  try {
    const res = await collectApi.collect(payload)
    const record = res.data
    pushRecent({
      code: record.collectCode,
      action: record.action,
      codeType: record.codeType,
      result: record.checkResult,
      message: record.duplicated ? '重复提交（幂等命中，未重复入库）' : record.checkMessage,
      time: record.collectTime,
    })
    if (record.checkResult === 'BLOCK') {
      ElMessage.error('该码已被防错规则拦截，详情见校验说明')
    } else {
      ElMessage.success('采集成功')
    }
    scanCode.value = ''
    await Promise.all([loadData(), loadStats()])
  } finally {
    submitting.value = false
    nextTick(() => scanInputRef.value?.focus?.())
  }
}

function pushRecent(item: { code: string; action: string; codeType: string; result: string; message: string; time: string }) {
  recent.value.unshift({ ...item, key: `${item.code}-${Date.now()}` })
  if (recent.value.length > 8) recent.value.pop()
}

async function flushQueue() {
  if (!queue.value.length) return
  uploading.value = true
  try {
    const res = await collectApi.batchCollect(queue.value)
    const summary = res.data as { accepted: number; duplicated: number; failed: number }
    ElMessage.success(`补传完成：新增 ${summary.accepted} 条，重复 ${summary.duplicated} 条，失败 ${summary.failed} 条`)
    queue.value = []
    await Promise.all([loadData(), loadStats()])
  } finally {
    uploading.value = false
  }
}

// ---------------- 补录 ----------------
const manualVisible = ref(false)
const manualForm = reactive({ code: '', action: 'STORE', reason: '' })

function submitManual() {
  if (!manualForm.code.trim() || !manualForm.reason.trim()) {
    ElMessage.warning('码与补录原因都不能为空')
    return
  }
  collectApi
    .collect({
      code: manualForm.code.trim(),
      action: manualForm.action as CollectPayload['action'],
      terminalType: 'WEB',
      source: 'MANUAL',
      reason: manualForm.reason,
      collectTime: formatNow(),
    })
    .then(() => {
      ElMessage.success('补录已提交，等待审核')
      manualVisible.value = false
      manualForm.code = ''
      manualForm.reason = ''
      loadData()
      loadStats()
    })
}

async function review(row: CollectRecord, approved: boolean) {
  let comment = ''
  try {
    const input = (await ElMessageBox.prompt(
      approved ? '确认该补录记录有效？' : '请填写驳回原因',
      approved ? '审核通过' : '审核驳回',
      { inputType: 'textarea', inputPlaceholder: '审核意见（可选）' },
    )) as { value: string }
    comment = input.value || ''
  } catch {
    return
  }
  await collectApi.review(row.collectId, approved, comment)
  ElMessage.success('审核完成')
  loadData()
  loadStats()
}

function formatNow(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<style scoped>
.collect-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-card span {
  font-size: 12px;
  color: #909399;
}

.stat-card strong {
  font-size: 22px;
  color: #1a5632;
}

.stat-card strong.danger {
  color: #c53030;
}

.stat-card strong.warning {
  color: #d97706;
}

.stat-card em {
  font-size: 12px;
  font-style: normal;
  color: #b1b3b8;
}

.collect-layout {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.scan-panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
  background: #fff;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1f2937;
}

.offline-bar {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff8e6;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  padding: 8px 10px;
  font-size: 12px;
  color: #8a5a00;
}

.scan-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.manual-entry {
  margin-top: 6px;
  text-align: right;
}

.recent-title {
  margin: 16px 0 8px;
  font-size: 13px;
  color: #606266;
}

.recent-empty {
  font-size: 12px;
  color: #c0c4cc;
}

.recent-item {
  border: 1px solid #ebeef5;
  border-left: 3px solid #909399;
  border-radius: 4px;
  padding: 8px 10px;
  margin-bottom: 8px;
  background: #fff;
}

.recent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.recent-code {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  word-break: break-all;
}

.recent-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.recent-msg {
  font-size: 12px;
  color: #c53030;
  margin-top: 4px;
  line-height: 1.5;
}

.record-panel {
  min-width: 0;
}

.text-muted {
  color: #c0c4cc;
}

.manual-alert {
  margin-bottom: 14px;
}
</style>
