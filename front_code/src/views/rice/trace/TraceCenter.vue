<template>
  <PageContainer title="追溯查询">
    <template #actions>
      <el-tag type="info" effect="plain">需求 §六-3 / §九 ZS-01·ZS-02</el-tag>
    </template>

    <!-- 检索区：一个输入框吃下所有码 -->
    <div class="trace-search">
      <el-input
        v-model="keyword"
        size="large"
        clearable
        placeholder="扫单品码 / 输入成品批次、加工批次、原粮批次、入库单、种植批次"
        @keyup.enter="doSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" size="large" :loading="loading" @click="doSearch">开始追溯</el-button>
      <el-button size="large" :disabled="!result" @click="openImpact">影响范围</el-button>
      <el-button
        v-if="result"
        size="large"
        type="danger"
        plain
        @click="goRecall"
      >
        发起召回
      </el-button>
    </div>

    <el-alert
      v-if="result && result.chain.tips.length"
      class="trace-tips"
      type="warning"
      :closable="false"
      show-icon
      title="链路提示"
    >
      <template #default>
        <div v-for="(tip, idx) in result.chain.tips" :key="idx">· {{ tip }}</div>
      </template>
    </el-alert>

    <template v-if="result">
      <!-- 命中信息 -->
      <div class="trace-hit">
        <el-tag effect="dark" type="success">{{ typeLabel(result.resolved.matchedType) }}</el-tag>
        <strong>{{ result.resolved.label }}</strong>
        <span class="trace-hit-note">{{ result.resolved.note }}</span>
      </div>

      <!-- 汇总指标 -->
      <div class="trace-summary">
        <div v-for="item in summaryItems" :key="item.label" class="trace-summary-card">
          <span class="label">{{ item.label }}</span>
          <strong class="value">{{ item.value }}</strong>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="trace-tabs">
        <!-- 链路图 -->
        <el-tab-pane label="链路图" name="chain">
          <div class="chain-flow">
            <div v-for="(group, gi) in stageGroups" :key="group.stage" class="chain-stage">
              <div class="stage-label">{{ group.stage }}</div>
              <div class="stage-nodes">
                <div
                  v-for="node in group.nodes"
                  :key="node.nodeKey"
                  class="chain-node"
                  :class="{ risky: node.risky }"
                  :style="{ borderTopColor: nodeColor(node.nodeType) }"
                  @click="openNode(node)"
                >
                  <div class="node-title">{{ node.title }}</div>
                  <div class="node-sub">{{ node.summary }}</div>
                  <div class="node-meta">
                    <span>{{ node.time || '时间待补' }}</span>
                    <StatusTag v-if="node.chainStatus" type="chain" :value="node.chainStatus" />
                  </div>
                </div>
              </div>
              <div v-if="gi < stageGroups.length - 1" class="stage-arrow">
                <el-icon><Right /></el-icon>
              </div>
            </div>
          </div>
          <p class="chain-legend">点击卡片查看该环节的完整字段、凭证与链上交易；箭头表示先后顺序，缺口说明该环节尚未录入。</p>
        </el-tab-pane>

        <!-- 时间轴 -->
        <el-tab-pane label="时间轴" name="timeline">
          <el-timeline class="trace-timeline">
            <el-timeline-item
              v-for="(item, idx) in result.chain.timeline"
              :key="idx"
              :timestamp="item.time || '时间待补'"
              placement="top"
              :type="item.risky ? 'danger' : 'primary'"
              :hollow="!item.risky"
            >
              <div class="timeline-head">
                <el-tag size="small" effect="plain">{{ item.stage }}</el-tag>
                <strong>{{ item.title }}</strong>
                <span v-if="item.organization" class="timeline-org">{{ item.organization }}</span>
                <StatusTag v-if="item.chainStatus" type="chain" :value="item.chainStatus" />
              </div>
              <div class="timeline-desc">{{ item.description }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>

        <!-- 链路关系（批次拆分/合并） -->
        <el-tab-pane label="链路关系" name="edges">
          <el-table :data="result.chain.edges" border stripe>
            <el-table-column prop="from" label="上游节点" min-width="200" />
            <el-table-column prop="label" label="关系" width="120" align="center" />
            <el-table-column prop="to" label="下游节点" min-width="200" />
            <el-table-column prop="quantity" label="流转量" width="140" align="center" />
          </el-table>
          <p class="chain-legend">关系类型：BOUND_TO 地块绑定 / HARVESTED_AS 收割入库 / MILLED_INTO 出库加工 / PACKED_AS 包装成品 / ACTIVATED_AS 赋码激活。</p>
        </el-tab-pane>

        <!-- 链上存证 -->
        <el-tab-pane label="链上存证" name="proof">
          <el-table :data="result.chain.chainProofs" border stripe>
            <el-table-column prop="stage" label="环节" width="90" align="center" />
            <el-table-column prop="businessType" label="业务类型" width="160" />
            <el-table-column prop="businessId" label="业务ID" min-width="160" />
            <el-table-column prop="dataHash" label="摘要哈希" min-width="200" show-overflow-tooltip />
            <el-table-column prop="txId" label="交易ID" min-width="200" show-overflow-tooltip />
            <el-table-column prop="blockHeight" label="区块高度" width="110" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag type="chain" :value="row.chainStatus" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!result.chain.chainProofs.length" description="该链路暂无存证记录（可能尚未上链）" />
        </el-tab-pane>

        <!-- 影响范围 -->
        <el-tab-pane label="影响范围" name="impact">
          <div v-if="impact">
            <div class="trace-summary">
              <div class="trace-summary-card">
                <span class="label">受影响防伪码</span>
                <strong class="value">{{ impact.traceCodeCount }}</strong>
              </div>
              <div class="trace-summary-card">
                <span class="label">已扫码（已售）</span>
                <strong class="value danger">{{ impact.scannedCodeCount }}</strong>
              </div>
              <div class="trace-summary-card">
                <span class="label">受影响重量(kg)</span>
                <strong class="value">{{ formatNumber(impact.weightKg) }}</strong>
              </div>
              <div class="trace-summary-card">
                <span class="label">已冻结</span>
                <strong class="value">{{ impact.frozenCodeCount }}</strong>
              </div>
            </div>

            <el-alert v-if="impact.tips.length" type="error" :closable="false" show-icon class="trace-tips">
              <template #default>
                <div v-for="(tip, idx) in impact.tips" :key="idx">· {{ tip }}</div>
              </template>
            </el-alert>

            <h4 class="trace-section-title">受影响成品批次</h4>
            <el-table :data="impact.productBatches" border stripe>
              <el-table-column prop="productBatchId" label="成品批次" min-width="150" />
              <el-table-column prop="productName" label="产品名称" min-width="140" />
              <el-table-column prop="brandName" label="品牌" width="120" />
              <el-table-column prop="packageSpec" label="规格" width="100" align="center" />
              <el-table-column prop="expectedSaleRegion" label="预期销售区域" min-width="140" />
              <el-table-column prop="traceCodeCount" label="防伪码" width="100" align="center" />
              <el-table-column prop="status" label="状态" width="110" align="center" />
            </el-table>

            <h4 class="trace-section-title">销售区域分布</h4>
            <el-table :data="impact.regions" border stripe>
              <el-table-column prop="region" label="区域" min-width="180" />
              <el-table-column prop="count" label="数量" width="120" align="center" />
              <el-table-column label="数据来源" width="140" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.source === 'SCAN' ? 'success' : 'info'">
                    {{ row.source === 'SCAN' ? '真实扫码' : '预期销售区' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-else class="trace-impact-empty">
            <el-button :loading="impactLoading" @click="loadImpact">计算影响范围</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-empty v-else description="支持扫码枪扫包装码、手工输入批次号，自动识别类型并还原全链路" />

    <!-- 节点详情 -->
    <el-drawer v-model="drawerVisible" :title="currentNode?.title || '节点详情'" size="520px">
      <template v-if="currentNode">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="节点类型">{{ typeLabel(currentNode.nodeType) }}</el-descriptions-item>
          <el-descriptions-item label="业务ID">{{ currentNode.businessId }}</el-descriptions-item>
          <el-descriptions-item label="发生时间">{{ currentNode.time || '-' }}</el-descriptions-item>
          <el-descriptions-item label="责任主体">{{ currentNode.organization || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ currentNode.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentNode.status || '-' }}</el-descriptions-item>
          <el-descriptions-item label="摘要">{{ currentNode.summary }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="trace-section-title">关键指标</h4>
        <el-table :data="metricsRows" border size="small">
          <el-table-column prop="label" label="指标" width="160" />
          <el-table-column prop="value" label="值" />
        </el-table>

        <template v-if="currentNode.txId">
          <h4 class="trace-section-title">链上存证</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="交易ID">{{ currentNode.txId }}</el-descriptions-item>
            <el-descriptions-item label="摘要哈希">{{ currentNode.dataHash || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>

        <div v-if="currentNode.detailRoute" class="drawer-actions">
          <el-button type="primary" plain @click="goDetail(currentNode.detailRoute)">查看业务详情</el-button>
        </div>
      </template>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { traceQueryApi } from '@/api/modules/traceQuery'
import type {
  TraceChainNode,
  TraceImpact,
  TraceSearchResult,
} from '@/types/traceQuery'
import { TRACE_NODE_COLOR, TRACE_STAGE_ORDER } from '@/types/traceQuery'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const impactLoading = ref(false)
const result = ref<TraceSearchResult | null>(null)
const impact = ref<TraceImpact | null>(null)
const activeTab = ref('chain')

const drawerVisible = ref(false)
const currentNode = ref<TraceChainNode | null>(null)

const TYPE_LABEL: Record<string, string> = {
  TRACE_CODE: '单品码',
  BOX_CODE: '箱码',
  PALLET_CODE: '托盘码',
  PRODUCT_BATCH: '成品批次',
  MILLING_BATCH: '加工批次',
  GRAIN_BATCH: '原粮批次',
  STORAGE_RECEIPT: '入库单',
  PLANTING_BATCH: '种植批次',
  FIELD: '地块',
  FARMING_LOG: '农事记录',
}

function typeLabel(type?: string): string {
  if (!type) return '-'
  return TYPE_LABEL[type] || type
}

function nodeColor(type?: string): string {
  return TRACE_NODE_COLOR[type || ''] || '#909399'
}

function formatNumber(value: unknown): string {
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const summaryItems = computed(() => {
  const s = result.value?.chain.summary || {}
  return [
    { label: '链路节点', value: s.nodeCount ?? 0 },
    { label: '农事记录', value: s.farmingLogCount ?? 0 },
    { label: '质检记录', value: s.qualityTestCount ?? 0 },
    { label: '防伪码', value: s.traceCodeCount ?? 0 },
    { label: '累计扫码', value: s.scanCount ?? 0 },
    { label: '上链成功率', value: `${s.chainSuccessRate ?? 0}%` },
  ]
})

/** 按环节分组，保证横向顺序固定为 种植 → 收储 → 加工 → 包装 → 流通 */
const stageGroups = computed(() => {
  const nodes = result.value?.chain.nodes || []
  const groups: { stage: string; nodes: TraceChainNode[] }[] = []
  for (const stage of TRACE_STAGE_ORDER) {
    const matched = nodes.filter((n) => n.stage === stage)
    if (matched.length) groups.push({ stage, nodes: matched })
  }
  const others = nodes.filter((n) => !(TRACE_STAGE_ORDER as readonly string[]).includes(n.stage))
  if (others.length) groups.push({ stage: '其他', nodes: others })
  return groups
})

const metricsRows = computed(() => {
  const metrics = currentNode.value?.metrics || {}
  return Object.entries(metrics).map(([label, value]) => ({
    label,
    value: value === null || value === undefined || value === ''
      ? '-'
      : typeof value === 'object'
        ? JSON.stringify(value)
        : String(value),
  }))
})

async function doSearch() {
  const key = keyword.value.trim()
  if (!key) {
    ElMessage.warning('请输入单品码或批次号')
    return
  }
  loading.value = true
  impact.value = null
  try {
    const res = await traceQueryApi.search(key)
    result.value = res.data
    activeTab.value = 'chain'
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}

async function loadImpact() {
  if (!result.value) return
  impactLoading.value = true
  activeTab.value = 'impact'
  try {
    const res = await traceQueryApi.impact(
      result.value.resolved.matchedType,
      result.value.resolved.matchedId,
    )
    impact.value = res.data
  } finally {
    impactLoading.value = false
  }
}

function openImpact() {
  if (impact.value) {
    activeTab.value = 'impact'
    return
  }
  loadImpact()
}

function openNode(node: TraceChainNode) {
  currentNode.value = node
  drawerVisible.value = true
}

function goDetail(route: string) {
  router.push(route)
}

function goRecall() {
  if (!result.value) return
  router.push({
    path: '/rice/recalls',
    query: {
      scopeType: result.value.resolved.matchedType,
      scopeId: result.value.resolved.matchedId,
    },
  })
}
</script>

<style scoped>
.trace-search {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.trace-search :deep(.el-input) {
  max-width: 620px;
}

.trace-tips {
  margin-bottom: 16px;
}

.trace-hit {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #f2f8f4;
  border-left: 3px solid #2f7a4d;
  border-radius: 4px;
  margin-bottom: 16px;
  font-size: 14px;
}

.trace-hit-note {
  color: #6b7280;
  font-size: 13px;
}

.trace-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.trace-summary-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.trace-summary-card .label {
  font-size: 12px;
  color: #909399;
}

.trace-summary-card .value {
  font-size: 20px;
  color: #1a5632;
}

.trace-summary-card .value.danger {
  color: #c53030;
}

.chain-flow {
  display: flex;
  align-items: stretch;
  gap: 4px;
  overflow-x: auto;
  padding: 8px 4px 16px;
}

.chain-stage {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stage-label {
  writing-mode: vertical-rl;
  font-size: 12px;
  letter-spacing: 4px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px 4px;
}

.stage-nodes {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 220px;
}

.chain-node {
  background: #fff;
  border: 1px solid #ebeef5;
  border-top: 3px solid #2f7a4d;
  border-radius: 6px;
  padding: 12px 14px;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.15s;
}

.chain-node:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.chain-node.risky {
  background: #fff5f5;
}

.node-title {
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
  margin-bottom: 6px;
}

.node-sub {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.node-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11px;
  color: #909399;
}

.stage-arrow {
  color: #c0c4cc;
  font-size: 20px;
  align-self: center;
  padding: 0 2px;
}

.chain-legend {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.trace-timeline {
  padding: 8px 0 0 4px;
}

.timeline-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.timeline-org {
  font-size: 12px;
  color: #909399;
}

.timeline-desc {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

.trace-section-title {
  margin: 18px 0 10px;
  font-size: 14px;
  color: #1f2937;
}

.trace-impact-empty {
  padding: 40px 0;
  text-align: center;
}

.drawer-actions {
  margin-top: 20px;
}
</style>
