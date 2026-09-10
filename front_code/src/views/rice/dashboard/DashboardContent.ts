import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { ElButton, ElCard, ElTag } from 'element-plus'
import type { EChartsOption } from 'echarts'
import PageContainer from '@/components/common/PageContainer.vue'
import EChart from '@/components/common/EChart.vue'
import { dashboardApi } from '@/api/modules/dashboard'
import type { DashboardSummary } from '@/types/dashboard'
import './dashboard.css'

const emptySummary: DashboardSummary = { fieldCount: 0, plantingBatchCount: 0, storedGrainWeightKg: 0, productBatchCount: 0, traceCodeCount: 0, scanCount: 0, riskWarningCount: 0, chainSuccessRate: 0 }

function barOption(labels: string[], values: number[], colors: string[]): EChartsOption {
  return { tooltip: { trigger: 'axis' }, grid: { left: 45, right: 20, top: 30, bottom: 35 }, xAxis: { type: 'category', data: labels }, yAxis: { type: 'value', minInterval: 1 }, series: [{ type: 'bar', barWidth: 34, label: { show: true, position: 'top' }, data: values.map((value, index) => ({ value, itemStyle: { color: colors[index], borderRadius: [6, 6, 0, 0] } })) }] }
}

export default defineComponent({
  name: 'DashboardContent',
  setup() {
    const loading = ref(false)
    const summary = ref<DashboardSummary>({ ...emptySummary })
    const cards = computed(() => [
      ['地块数量', summary.value.fieldCount, '已建档地块'],
      ['种植批次', summary.value.plantingBatchCount, '生产批次'],
      ['原粮入库', `${(summary.value.storedGrainWeightKg / 1000).toFixed(1)}t`, '累计湿谷重量'],
      ['成品批次', summary.value.productBatchCount, '包装批次'],
      ['防伪码', summary.value.traceCodeCount, '一物一码'],
      ['累计扫码', summary.value.scanCount, '消费者查询'],
      ['风险预警', summary.value.riskWarningCount, '监管待核查'],
      ['上链成功率', `${summary.value.chainSuccessRate}%`, '链上存证'],
    ])
    const assetOption = computed(() => barOption(['地块', '种植', '成品', '防伪码'], [summary.value.fieldCount, summary.value.plantingBatchCount, summary.value.productBatchCount, summary.value.traceCodeCount], ['#2f7a4d', '#65a765', '#e7b84b', '#d97706']))
    const scanOption = computed(() => barOption(['防伪码', '扫码', '预警'], [summary.value.traceCodeCount, summary.value.scanCount, summary.value.riskWarningCount], ['#2f7a4d', '#e7b84b', '#c53030']))
    const riskOption = computed<EChartsOption>(() => ({ tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['48%', '72%'], center: ['50%', '44%'], data: [{ name: '正常防伪码', value: Math.max(0, summary.value.traceCodeCount - summary.value.riskWarningCount), itemStyle: { color: '#65a765' } }, { name: '风险预警', value: summary.value.riskWarningCount, itemStyle: { color: '#c53030' } }] }] }))
    const chainOption = computed<EChartsOption>(() => ({ series: [{ type: 'gauge', startAngle: 210, endAngle: -30, progress: { show: true, width: 16, itemStyle: { color: '#2f7a4d' } }, axisLine: { lineStyle: { width: 16, color: [[1, '#e8efea']] } }, pointer: { show: false }, axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false }, detail: { formatter: '{value}%', fontSize: 28, color: '#1a5632' }, data: [{ value: summary.value.chainSuccessRate }] }] }))

    async function loadSummary() {
      loading.value = true
      try { summary.value = (await dashboardApi.getSummary()).data } finally { loading.value = false }
    }

    function chartCard(title: string, option: EChartsOption) {
      return h('section', { class: 'dashboard-chart-card' }, [h('h3', title), h(EChart, { option })])
    }

    onMounted(loadSummary)
    return () => h(PageContainer, { title: '数据看板' }, {
      actions: () => h(ElButton, { loading: loading.value, onClick: loadSummary }, () => '刷新数据'),
      default: () => [
        h('div', { class: 'dashboard-overview' }, cards.value.map(([label, value, note]) => h(ElCard, { shadow: 'hover', class: 'dashboard-stat-card' }, () => [h('span', { class: 'dashboard-stat-label' }, String(label)), h('strong', { class: 'dashboard-stat-value' }, String(value)), h('div', { class: 'dashboard-stat-note' }, String(note))]))),
        h('div', { class: 'dashboard-chart-grid' }, [chartCard('业务资产规模', assetOption.value), chartCard('链上存证成功率', chainOption.value), chartCard('防伪码风险构成', riskOption.value), chartCard('扫码转化概览', scanOption.value)]),
        h('div', { class: 'dashboard-footnote' }, [h('span', '数据来源：GET /rice/dashboard/summary'), h(ElTag, { type: 'success' }, () => '更新至 2026-07-26')]),
      ],
    })
  },
})
