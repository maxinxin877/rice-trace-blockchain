<template>
  <PageContainer title="数据看板">
    <template #actions>
      <div class="global-search">
        <el-input
          v-model="searchQuery"
          placeholder="输入批次号/产品号/防伪码查询"
          class="search-input"
          clearable
          @keyup.enter="doSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </template>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 24px">
      <el-col :span="6" v-for="item in statCards" :key="item.label">
        <div class="stat-card">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" style="margin-bottom: 24px">
      <!-- 链上状态 -->
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">链上状态分布</div>
          <div class="pie-chart-wrapper">
            <div class="pie-container" @mouseleave="hoveredIdx = -1">
              <svg viewBox="-120 -120 240 240" class="pie-svg">
                <path
                  v-for="(item, idx) in pieSegments"
                  :key="item.label"
                  :d="item.path"
                  :fill="item.color"
                  :transform="hoveredIdx === idx ? `translate(${item.offsetX * 8}, ${item.offsetY * 8})` : ''"
                  class="pie-slice"
                  @mouseenter="hoveredIdx = idx"
                  @mousemove="onSliceMove"
                  @mouseleave="hoveredIdx = -1"
                />
                <!-- 中心白色圆（甜甜圈效果） -->
                <circle cx="0" cy="0" r="50" fill="#fff" />
              </svg>
              <!-- tooltip -->
              <div
                v-if="hoveredIdx >= 0 && tooltipPos.x"
                class="pie-tooltip"
                :style="{ left: tooltipPos.x + 'px', top: tooltipPos.y + 'px' }"
              >
                <div class="tooltip-label">{{ chainData[hoveredIdx]?.label }}</div>
                <div class="tooltip-num">{{ chainData[hoveredIdx]?.count }} 条 ({{ chainData[hoveredIdx]?.percent }}%)</div>
              </div>
            </div>
            <div class="pie-legend">
              <div class="legend-item" v-for="(item, idx) in chainData" :key="item.label" @mouseenter="hoveredIdx = idx" @mouseleave="hoveredIdx = -1">
                <span class="legend-dot" :style="{ backgroundColor: item.color }"></span>
                <span class="legend-label">{{ item.label }}</span>
                <span class="legend-value">{{ item.count }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 批次状态 -->
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">批次状态分布</div>
          <div class="bar-chart">
            <div class="bar-row" v-for="item in batchData" :key="item.label">
              <span class="bar-label">{{ item.label }}</span>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: item.percent + '%', backgroundColor: item.color }"></div>
              </div>
              <span class="bar-num">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 预警概览 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">风险预警概览</div>
          <div class="vbar-chart">
            <div class="vbar-col" v-for="item in warningSummary" :key="item.label">
              <span class="vbar-num">{{ item.count }}</span>
              <div class="vbar-track">
                <div class="vbar-fill" :style="{ height: item.max ? (item.count / item.max * 100) + '%' : '0%', backgroundColor: item.color }"></div>
              </div>
              <span class="vbar-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">扫码统计</div>
          <div class="scan-summary">
            <div class="scan-big-num">{{ scanStats.total }}</div>
            <div class="scan-sub">累计扫码次数</div>
            <div class="scan-detail">
              <span>已激活码 {{ scanStats.activated }}</span>
              <span class="sep">|</span>
              <span>待激活码 {{ scanStats.generated }}</span>
              <span class="sep">|</span>
              <span>风险码 {{ scanStats.risk }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </PageContainer>

  <!-- 防伪码溯源结果弹窗 -->
  <el-dialog v-model="showTraceDialog" title="防伪码溯源查询" width="800px" top="5vh">
    <template v-if="traceResult">
      <div class="trace-summary">
        <el-tag type="danger" size="large">防伪码: {{ traceResult.traceCode }}</el-tag>
        <el-tag :type="traceResult.status === 'ACTIVATED' ? 'success' : 'warning'" style="margin-left: 8px">{{ traceResult.status === 'ACTIVATED' ? '已激活' : traceResult.status }}</el-tag>
      </div>
      <div class="trace-chain-vertical">
        <div class="chain-node" v-for="(node, idx) in traceResult.chain" :key="idx">
          <div class="chain-connector" v-if="idx > 0"></div>
          <div class="chain-card" :style="{ borderLeftColor: node.color }">
            <div class="chain-card-header">
              <span class="chain-step" :style="{ background: node.color }">{{ idx + 1 }}</span>
              <span class="chain-label">{{ node.label }}</span>
              <span class="chain-id">{{ node.id }}</span>
            </div>
            <div class="chain-card-body">
              <div class="chain-field" v-for="(v, k) in node.fields" :key="k">
                <span class="chain-key">{{ k }}</span>
                <span class="chain-val">{{ v }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { computed, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const searchQuery = ref('')

// Mock 批次号 → 页面路由映射
const batchMap: Record<string, string> = {
  RPB202607010001: '/rice/planting-batches/detail/RPB202607010001',
  RPB202607010002: '/rice/planting-batches/detail/RPB202607010002',
  RPB202607010003: '/rice/planting-batches/detail/RPB202607010003',
  RPB202607010004: '/rice/planting-batches/detail/RPB202607010004',
  RPB202607010005: '/rice/planting-batches/detail/RPB202607010005',
  PROD20261001001: '/rice/product-batches',
  PROD20261001002: '/rice/product-batches',
  PROD20261001003: '/rice/product-batches',
  FIELD202607010001: '/rice/fields/detail/FIELD202607010001',
  FIELD202607010002: '/rice/fields/detail/FIELD202607010002',
  SREC202607010001: '/rice/storage-receipts',
  MB202607010001: '/rice/milling-batches',
  RC20261001: '/rice/trace-codes',
}

function doSearch() {
  const q = searchQuery.value.trim()
  if (!q) { ElMessage.warning('请输入查询号码'); return }
  
  // 防伪码查询 → 弹窗展示全链路
  if (q.startsWith('RC') || traceCodeMap[q]) {
    showTraceResult(q)
    return
  }
  
  // 批次号跳转
  if (batchMap[q]) { router.push(batchMap[q]); return }
  for (const [key, path] of Object.entries(batchMap)) {
    if (key.includes(q) || q.includes(key)) { router.push(path); return }
  }
  ElMessage.info('未找到匹配的批次，请检查号码是否正确')
}

// ========== 防伪码全链路溯源 ==========
interface ChainNode {
  label: string; id: string; color: string
  fields: Record<string, string>
}

interface TraceResult {
  traceCode: string; status: string
  chain: ChainNode[]
}

const showTraceDialog = ref(false)
const traceResult = ref<TraceResult | null>(null)

// Mock 防伪码 → 全链路数据
const traceCodeMap: Record<string, TraceResult> = {
  RC20261001000001: {
    traceCode: 'RC20261001000001', status: 'ACTIVATED',
    chain: [
      { label: '地块档案', id: 'FIELD202607010001', color: '#409EFF', fields: { '地块名称': '五常一号稻田', '种植户': '张三丰', '面积': '150.50 亩', '土壤': '黑土', '坐标': '127.5678, 45.1234' } },
      { label: '种植批次', id: 'RPB202607010001', color: '#67C23A', fields: { '品种': '五优稻4号', '种子来源': '五常市种子公司', '播种': '2026-05-01', '收割': '2026-09-20', '有机认证': '✅' } },
      { label: '农事记录', id: 'FLOG202607010001~005', color: '#00BCD4', fields: { '播种': '2026-05-01 / 张三丰', '施肥': '2026-06-01 / 复合肥 25kg/亩', '用药': '2026-07-01 / 吡虫啉 安全间隔14天', '灌溉': '2026-07-15 / 保持3cm水层', '收割': '2026-09-20 / 联合收割机 150亩' } },
      { label: '环境数据', id: 'ENV202607010001~004', color: '#FF9800', fields: { 'IOT气温': '28~32℃', '空气湿度': '65~80%', '土壤湿度': '32~40%', '降雨': '0~12mm' } },
      { label: '收储入库', id: 'SREC202607010001', color: '#E6A23C', fields: { '原粮批次': 'GRAIN20260920001', '入仓编号': 'WC-01-001', '湿谷重量': '97,500 kg', '水分': '25.50%', '杂质': '2.00%', '等级': '一等' } },
      { label: '入库质检', id: 'QTEST202607010001', color: '#9C27B0', fields: { '检测机构': '黑龙江省粮油质量检测中心', '结论': '✅ 合格', '水分': '14.50%', '杂质': '0.80%', '出糙率': '79%', '整精米率': '58%' } },
      { label: '碾米加工', id: 'MB202607010001', color: '#F56C6C', fields: { '原粮出库': '50,000 kg', '精米产出': '32,500 kg', '产出率': '65%', '脱壳': '胶辊砻谷机', '抛光': '三道抛光', '包装': '真空包装 5kg/袋' } },
      { label: '成品批次', id: 'PROD20261001001', color: '#1a5632', fields: { '产品名称': '五常有机稻花香大米', '品牌': '五常御品', '规格': '5kg/袋', '标准号': 'GB/T 19266', '销售区域': '北京、上海、广州、深圳' } },
    ],
  },
  RC20261001000002: {
    traceCode: 'RC20261001000002', status: 'ACTIVATED',
    chain: [
      { label: '地块档案', id: 'FIELD202607010001', color: '#409EFF', fields: { '地块名称': '五常一号稻田', '种植户': '张三丰' } },
      { label: '种植批次', id: 'RPB202607010001', color: '#67C23A', fields: { '品种': '五优稻4号' } },
      { label: '收储入库', id: 'SREC202607010001', color: '#E6A23C', fields: { '原粮批次': 'GRAIN20260920001', '等级': '一等' } },
      { label: '碾米加工', id: 'MB202607010001', color: '#F56C6C', fields: { '产出率': '65%', '包装': '5kg/袋' } },
      { label: '成品批次', id: 'PROD20261001001', color: '#1a5632', fields: { '产品名称': '五常有机稻花香大米', '品牌': '五常御品' } },
    ],
  },
  RC20261001000008: {
    traceCode: 'RC20261001000008', status: 'RISK',
    chain: [
      { label: '地块档案', id: 'FIELD202607010002', color: '#409EFF', fields: { '地块名称': '五常二号稻田', '种植户': '李四喜' } },
      { label: '种植批次', id: 'RPB202607010002', color: '#67C23A', fields: { '品种': '稻花香2号' } },
      { label: '收储入库', id: 'SREC202607010002', color: '#E6A23C', fields: { '原粮批次': 'GRAIN20260925001' } },
      { label: '碾米加工', id: 'MB202607010003', color: '#F56C6C', fields: { '工厂': 'FACTORY002', '工艺': '四道抛光/AI色选' } },
      { label: '成品批次', id: 'PROD20261001003', color: '#1a5632', fields: { '产品名称': '稻花香2号精米', '品牌': '龙江香米' } },
    ],
  },
}

function showTraceResult(code: string) {
  // 精确匹配
  if (traceCodeMap[code]) {
    traceResult.value = traceCodeMap[code]
    showTraceDialog.value = true
    return
  }
  // 模糊匹配
  for (const [key, val] of Object.entries(traceCodeMap)) {
    if (key.includes(code) || code.includes(key)) {
      traceResult.value = val
      showTraceDialog.value = true
      return
    }
  }
  // 没找到，生成默认链路
  traceResult.value = {
    traceCode: code, status: 'UNKNOWN',
    chain: [
      { label: '防伪码', id: code, color: '#F56C6C', fields: { '状态': '未在系统中找到该防伪码的关联信息', '提示': '请检查防伪码是否正确' } },
    ],
  }
  showTraceDialog.value = true
}

const statCards = [
  { label: '地块总数', value: 5 },
  { label: '种植批次', value: 5 },
  { label: '入库单数', value: 3 },
  { label: '成品批次', value: 4 },
]

const chainData = [
  { label: '已上链', count: 15, percent: 60, color: '#67C23A' },
  { label: '待上链', count: 8, percent: 32, color: '#909399' },
  { label: '上链失败', count: 2, percent: 8, color: '#F56C6C' },
]

const batchData = [
  { label: '种植中', count: 2, percent: 40, color: '#409EFF' },
  { label: '已收割', count: 1, percent: 20, color: '#E6A23C' },
  { label: '已入库', count: 1, percent: 20, color: '#67C23A' },
  { label: '加工中', count: 1, percent: 20, color: '#909399' },
]

const warningSummary = [
  { label: '高风险', count: 2, color: '#F56C6C', max: 3 },
  { label: '中风险', count: 1, color: '#E6A23C', max: 3 },
  { label: '低风险', count: 0, color: '#909399', max: 3 },
  { label: '已处理', count: 2, color: '#67C23A', max: 3 },
]

const scanStats = { total: 555, activated: 4, generated: 2, risk: 2 }

// ========== 饼图逻辑 ==========
const hoveredIdx = ref(-1)
const tooltipPos = reactive({ x: 0, y: 0 })

// 根据圆心角计算 SVG arc 路径
function polarToCartesian(cx: number, cy: number, r: number, angleDeg: number) {
  const rad = (angleDeg - 90) * Math.PI / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}

function describeArc(cx: number, cy: number, r: number, startAngle: number, endAngle: number) {
  const start = polarToCartesian(cx, cy, r, endAngle)
  const end = polarToCartesian(cx, cy, r, startAngle)
  const largeArc = endAngle - startAngle > 180 ? 1 : 0
  return `M ${cx} ${cy} L ${start.x} ${start.y} A ${r} ${r} 0 ${largeArc} 0 ${end.x} ${end.y} Z`
}

const pieSegments = computed(() => {
  const r = 95
  let startAngle = 0
  return chainData.map((item) => {
    const angle = item.percent / 100 * 360
    const endAngle = startAngle + angle
    const path = describeArc(0, 0, r, startAngle, endAngle)
    // 扇区中心方向用于 hover 偏移
    const midAngle = (startAngle + endAngle) / 2
    const rad = (midAngle - 90) * Math.PI / 180
    const offsetX = Math.cos(rad)
    const offsetY = Math.sin(rad)
    const seg = { ...item, path, offsetX, offsetY }
    startAngle = endAngle
    return seg
  })
})

function onSliceMove(e: MouseEvent) {
  const rect = (e.currentTarget as SVGElement).closest('.pie-container')?.getBoundingClientRect()
  if (rect) {
    tooltipPos.x = e.clientX - rect.left + 16
    tooltipPos.y = e.clientY - rect.top - 36
  }
}
</script>

<style scoped>
/* 全局搜索 */
.global-search {
  display: flex;
  align-items: center;
}
.search-input {
  width: 320px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  padding: 20px 24px;
  text-align: center;
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  border-color: #c9cdd4;
}
.stat-value { font-size: 32px; font-weight: 600; color: #1d2129; line-height: 1.2; }
.stat-label { font-size: 13px; color: #86909c; margin-top: 6px; }

.chart-card {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  padding: 20px 24px;
  height: 100%;
  transition: box-shadow 0.3s ease;
}
.chart-card:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}
.chart-title { font-size: 15px; font-weight: 600; color: #1d2129; margin-bottom: 20px; }

/* 柱状图 */
.bar-chart { display: flex; flex-direction: column; gap: 20px; }
.bar-row { display: flex; align-items: center; gap: 12px; padding: 4px 6px; border-radius: 6px; transition: background 0.2s; cursor: pointer; }
.bar-row:hover { background: #f7f8fa; }
.bar-label { width: 70px; font-size: 13px; color: #4e5969; text-align: right; flex-shrink: 0; }
.bar-track { flex: 1; height: 22px; background: #f2f3f5; border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 4px; transition: width 0.6s ease, filter 0.2s; min-width: 4px; }
.bar-fill:hover { filter: brightness(1.2); }
.bar-num { width: 36px; font-size: 13px; color: #1d2129; font-weight: 600; text-align: right; }

/* 竖状柱图 */
.vbar-chart {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 160px;
  padding-top: 8px;
}
.vbar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: background 0.2s;
}
.vbar-col:hover {
  background: #f7f8fa;
}
.vbar-col:hover .vbar-num {
  transform: scale(1.15);
}
.vbar-col:hover .vbar-track {
  transform: scaleX(1.12);
}
.vbar-num {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  transition: transform 0.25s ease;
}
.vbar-track {
  width: 48px;
  height: 100px;
  background: #f2f3f5;
  border-radius: 6px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
  transition: transform 0.25s ease;
}
.vbar-fill {
  width: 100%;
  border-radius: 6px;
  transition: height 0.5s ease, filter 0.2s;
  min-height: 4px;
}
.vbar-fill:hover { filter: brightness(1.2); }
.vbar-label {
  font-size: 12px;
  color: #86909c;
}

/* 扫码 */
.scan-summary { text-align: center; }
.scan-big-num { font-size: 48px; font-weight: 700; color: #1a5632; line-height: 1.2; }
.scan-sub { font-size: 13px; color: #86909c; margin: 8px 0 16px; }
.scan-detail { font-size: 13px; color: #4e5969; }
.scan-detail .sep { margin: 0 10px; color: #e5e6eb; }

/* 饼图 */
.pie-chart-wrapper {
  display: flex;
  align-items: center;
  gap: 28px;
}
.pie-container {
  position: relative;
  width: 220px;
  height: 220px;
  flex-shrink: 0;
}
.pie-svg {
  width: 100%;
  height: 100%;
}
.pie-slice {
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.3s ease;
}
.pie-slice:hover {
  filter: brightness(1.15) drop-shadow(0 2px 6px rgba(0,0,0,0.15));
}
.pie-tooltip {
  position: absolute;
  pointer-events: none;
  background: rgba(29, 33, 41, 0.9);
  color: #fff;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 12px;
  white-space: nowrap;
  z-index: 10;
  transition: opacity 0.15s ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.tooltip-label {
  font-weight: 600;
  font-size: 13px;
}
.tooltip-num {
  color: #c9cdd4;
  margin-top: 3px;
}
.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background 0.2s;
}
.legend-item:hover {
  background: #f7f8fa;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}
.legend-label {
  color: #4e5969;
  flex: 1;
}
.legend-value {
  color: #1d2129;
  font-weight: 600;
}

/* 防伪码溯源链 */
.trace-summary { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #e5e6eb; }
.trace-chain-vertical { display: flex; flex-direction: column; }
.chain-connector { width: 2px; height: 20px; background: #d0d5dd; margin-left: 20px; }
.chain-card { border: 1px solid #e5e6eb; border-left: 4px solid #409EFF; border-radius: 6px; padding: 12px 16px; background: #fff; transition: box-shadow 0.2s; }
.chain-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.chain-card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.chain-step { width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.chain-label { font-size: 14px; font-weight: 600; color: #1d2129; }
.chain-id { font-size: 11px; color: #c9cdd4; font-family: monospace; margin-left: auto; }
.chain-card-body { display: flex; flex-wrap: wrap; gap: 4px 20px; }
.chain-field { font-size: 12px; }
.chain-key { color: #86909c; }
.chain-key::after { content: ': '; }
.chain-val { color: #1d2129; }
</style>
