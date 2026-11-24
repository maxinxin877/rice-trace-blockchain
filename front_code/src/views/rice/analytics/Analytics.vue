<template>
  <PageContainer title="质量分析">
    <template #actions>
      <el-button :loading="loading" @click="load">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </template>

    <div v-loading="loading">
      <template v-if="data">
        <!-- 风险评分 -->
        <div class="analysis-top">
          <el-card shadow="hover" class="risk-card">
            <div class="risk-head">
              <div>
                <span class="risk-label">质量风险评分</span>
                <strong class="risk-score" :style="{ color: levelColor }">{{ data.riskProfile.score }}</strong>
                <el-tag size="small" :color="levelColor" :style="{ color: '#fff', border: 'none' }">
                  {{ RISK_LEVEL_LABEL[data.riskProfile.level] }}
                </el-tag>
              </div>
              <span class="risk-method">{{ data.riskProfile.calculatedAt }}</span>
            </div>
            <EChart :option="gaugeOption" class="risk-gauge" />
            <p class="risk-method">评分口径：{{ data.riskProfile.method }}</p>
          </el-card>

          <el-card shadow="hover" class="factor-card">
            <template #header>
              <span class="card-title">主要风险因素</span>
            </template>
            <div v-if="data.riskProfile.factors.length" class="factor-list">
              <div v-for="(factor, idx) in data.riskProfile.factors" :key="idx" class="factor-item">
                <el-icon class="factor-icon"><WarningFilled /></el-icon>
                <span>{{ factor }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无显著风险因素" :image-size="70" />
          </el-card>

          <el-card shadow="hover" class="factor-card">
            <template #header>
              <span class="card-title">处置建议</span>
            </template>
            <div class="factor-list">
              <div v-for="(item, idx) in data.riskProfile.suggestions" :key="idx" class="factor-item">
                <el-icon class="factor-icon success"><CircleCheckFilled /></el-icon>
                <span>{{ item }}</span>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 召回绩效 -->
        <div class="perf-grid">
          <div class="perf-card">
            <span>召回任务</span>
            <strong>{{ perf.taskCount }}</strong>
            <em>已发布 {{ perf.releasedCount }} / 已关闭 {{ perf.closedCount }}</em>
          </div>
          <div class="perf-card">
            <span>关闭率</span>
            <strong>{{ perf.closeRate }}%</strong>
            <em>处置闭环程度</em>
          </div>
          <div class="perf-card">
            <span>回收率</span>
            <strong>{{ perf.recoveryRate }}%</strong>
            <em>已处置 {{ perf.itemCount - perf.unRecoveredCount }} / {{ perf.itemCount }}</em>
          </div>
          <div class="perf-card">
            <span>平均处置周期</span>
            <strong>{{ perf.avgHandleHours }}h</strong>
            <em>发布 → 关闭</em>
          </div>
          <div class="perf-card">
            <span>未召回</span>
            <strong class="danger">{{ perf.unRecoveredCount }}</strong>
            <em>需继续跟踪</em>
          </div>
        </div>

        <!-- 图表 -->
        <div class="chart-grid">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">质检合格趋势（近 6 个月，FX-01）</span>
            </template>
            <EChart :option="trendOption" />
          </el-card>
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">异常类型分布（FX-01/FX-02）</span>
            </template>
            <EChart :option="warningOption" />
          </el-card>
        </div>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="analysis-note"
          title="数据来源：rice_quality_test / rice_risk_warning / rice_recall_task / rice_recall_item / rice_farming_log / rice_milling_batch / rice_trace_code"
          description="当前客诉以风险预警类型归因（暂无独立投诉表）；风险评分为规则加权，后续可替换为模型评分，口径保持一致即可。"
        />
      </template>
      <el-empty v-else description="暂无分析数据" />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import PageContainer from '@/components/common/PageContainer.vue'
import EChart from '@/components/common/EChart.vue'
import { analyticsApi } from '@/api/modules/analytics'
import type { AnalyticsOverview } from '@/types/analytics'
import { RISK_LEVEL_LABEL } from '@/types/analytics'

const loading = ref(false)
const data = ref<AnalyticsOverview | null>(null)

const perf = computed(() => data.value?.recallPerformance ?? {
  taskCount: 0,
  releasedCount: 0,
  closedCount: 0,
  closeRate: 0,
  avgHandleHours: 0,
  recoveryRate: 0,
  itemCount: 0,
  unRecoveredCount: 0,
})

const levelColor = computed(() => {
  const level = data.value?.riskProfile.level
  return level === 'HIGH' ? '#c53030' : level === 'MEDIUM' ? '#d97706' : '#2f7a4d'
})

const gaugeOption = computed<EChartsOption>(() => ({
  series: [{
    type: 'gauge',
    startAngle: 210,
    endAngle: -30,
    min: 0,
    max: 100,
    progress: { show: true, width: 14, itemStyle: { color: levelColor.value } },
    axisLine: { lineStyle: { width: 14, color: [[1, '#e8efea']] } },
    pointer: { show: false },
    axisTick: { show: false },
    splitLine: { show: false },
    axisLabel: { show: false },
    detail: { show: false },
    data: [{ value: data.value?.riskProfile.score ?? 0 }],
  }],
}))

const trendOption = computed<EChartsOption>(() => {
  const trend = data.value?.qualityTrend ?? []
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 40, right: 20, top: 24, bottom: 48 },
    xAxis: { type: 'category', data: trend.map((t) => t.month) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '合格',
        type: 'bar',
        stack: 'total',
        barWidth: 26,
        itemStyle: { color: '#2f7a4d', borderRadius: [0, 0, 0, 0] },
        data: trend.map((t) => t.passCount),
      },
      {
        name: '不合格',
        type: 'bar',
        stack: 'total',
        itemStyle: { color: '#c53030', borderRadius: [4, 4, 0, 0] },
        data: trend.map((t) => t.failCount),
        label: {
          show: true,
          position: 'top',
          formatter: (params: { dataIndex: number }) => `${trend[params.dataIndex]?.passRate ?? 0}%`,
          color: '#6b7280',
          fontSize: 11,
        },
      },
    ],
  }
})

const warningOption = computed<EChartsOption>(() => {
  const list = data.value?.warningDistribution ?? []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['45%', '68%'],
      center: ['50%', '44%'],
      label: { formatter: '{b}\n{c}' },
      data: list.map((item, index) => ({
        name: item.label,
        value: item.count,
        itemStyle: { color: PALETTE[index % PALETTE.length] },
      })),
    }],
  }
})

const PALETTE = ['#2f7a4d', '#65a765', '#e7b84b', '#d97706', '#c53030', '#7a5af8']

async function load() {
  loading.value = true
  try {
    const res = await analyticsApi.overview()
    data.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.analysis-top {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.risk-card {
  display: flex;
  flex-direction: column;
}

.risk-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.risk-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.risk-score {
  font-size: 34px;
  font-weight: 700;
  margin-right: 8px;
}

.risk-gauge {
  height: 150px !important;
  min-height: 150px;
}

.risk-method {
  font-size: 11px;
  color: #b1b3b8;
  line-height: 1.6;
  margin: 6px 0 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.factor-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.factor-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

.factor-icon {
  color: #c53030;
  margin-top: 2px;
  flex-shrink: 0;
}

.factor-icon.success {
  color: #2f7a4d;
}

.perf-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.perf-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.perf-card span {
  font-size: 12px;
  color: #909399;
}

.perf-card strong {
  font-size: 22px;
  color: #1a5632;
}

.perf-card strong.danger {
  color: #c53030;
}

.perf-card em {
  font-size: 12px;
  font-style: normal;
  color: #b1b3b8;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.chart-grid :deep(.echart) {
  height: 300px;
}

.analysis-note {
  margin-top: 16px;
}
</style>
