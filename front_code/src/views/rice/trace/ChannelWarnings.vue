<template>
  <PageContainer title="窜货与扫码区域预警">
    <template #actions><el-button :loading="loading" @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button></template>
    <el-alert title="根据防伪码预期销售区域与实际扫码地区生成窜货预警，并结合扫码频次评估风险。" type="info" :closable="false" show-icon />
    <div class="metrics">
      <div><strong>{{ warnings.length }}</strong><span>预警总数</span></div>
      <div><strong class="danger">{{ highRiskCount }}</strong><span>高风险</span></div>
      <div><strong class="warning">{{ pendingCount }}</strong><span>待处理</span></div>
      <div><strong>{{ regionCount }}</strong><span>涉及异地</span></div>
    </div>
    <div class="layout">
      <div class="region-board">
        <h3>异常扫码地区分布</h3>
        <div v-for="item in regionSummary" :key="item.region" class="region-row">
          <span>{{ item.region }}</span><div class="bar"><i :style="{ width: item.percent + '%' }" /></div><b>{{ item.count }}</b>
        </div>
        <div class="route"><span>预期销售区</span><b>→</b><span>异常扫码区</span></div>
      </div>
      <el-table v-loading="loading" :data="warnings" border stripe>
        <el-table-column prop="warningTime" label="预警时间" width="170" />
        <el-table-column prop="traceCode" label="防伪码" width="180" />
        <el-table-column prop="productName" label="产品" min-width="150" show-overflow-tooltip />
        <el-table-column prop="expectedRegion" label="预期区域" min-width="130" />
        <el-table-column prop="actualRegion" label="实际扫码地" min-width="130" />
        <el-table-column prop="scanCount" label="扫码次数" width="90" />
        <el-table-column label="风险" width="90"><template #default="{ row }"><StatusTag type="risk" :value="row.riskLevel" /></template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.handled ? 'success' : 'warning'">{{ row.handled ? '已处理' : '待处理' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button type="primary" link @click="goHandle(row.traceCode)">处置</el-button></template></el-table-column>
      </el-table>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { traceabilityApi } from '@/api/modules/traceability'
import type { ChannelWarningRecord } from '@/types/traceability'

const router = useRouter()
const loading = ref(false)
const warnings = ref<ChannelWarningRecord[]>([])
const highRiskCount = computed(() => warnings.value.filter((item) => item.riskLevel === 'HIGH').length)
const pendingCount = computed(() => warnings.value.filter((item) => !item.handled).length)
const regionCount = computed(() => new Set(warnings.value.map((item) => item.actualRegion)).size)
const regionSummary = computed(() => {
  const counter = new Map<string, number>()
  warnings.value.forEach((item) => counter.set(item.actualRegion, (counter.get(item.actualRegion) || 0) + 1))
  const max = Math.max(...counter.values(), 1)
  return [...counter.entries()].map(([region, count]) => ({ region, count, percent: Math.round(count / max * 100) }))
})
async function loadData() { loading.value = true; try { warnings.value = (await traceabilityApi.getChannelWarnings()).data } finally { loading.value = false } }
function goHandle(businessId: string) { router.push({ path: '/rice/regulation/risk-warnings', query: { businessId } }) }
onMounted(loadData)
</script>

<style scoped>
.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:16px 0}.metrics>div{padding:17px;border:1px solid #e5e6eb;border-radius:8px}.metrics strong{display:block;font-size:28px;color:#1a5632}.metrics span{color:#86909c;font-size:13px}.danger{color:#c53030!important}.warning{color:#d97706!important}.layout{display:grid;grid-template-columns:290px minmax(0,1fr);gap:16px}.region-board{padding:20px;border-radius:10px;color:#fff;background:radial-gradient(circle at 20% 10%,#397c56,#143f27)}.region-board h3{margin:0 0 20px}.region-row{display:grid;grid-template-columns:90px 1fr 20px;gap:8px;align-items:center;margin:15px 0;font-size:12px}.bar{height:7px;background:#ffffff2e;border-radius:8px;overflow:hidden}.bar i{display:block;height:100%;background:#f5c451}.route{display:flex;justify-content:space-between;align-items:center;margin-top:28px;padding-top:18px;border-top:1px solid #ffffff2e;font-size:12px}.route span{padding:7px 9px;border-radius:15px;background:#ffffff1f}@media(max-width:1200px){.layout{grid-template-columns:1fr}.metrics{grid-template-columns:repeat(2,1fr)}}
</style>
