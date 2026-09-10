<template>
  <PageContainer title="产量平衡校验">
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :model="form" inline>
        <el-form-item label="校验范围">
          <el-select v-model="form.scope" style="width: 200px">
            <el-option label="全量校验" value="ALL" />
            <el-option label="按种植批次" value="BATCH" />
            <el-option label="按地块" value="FIELD" />
          </el-select>
        </el-form-item>
        <el-form-item label="种植批次" v-if="form.scope === 'BATCH'">
          <el-input v-model="form.batchId" placeholder="输入批次ID" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="runCheck" :loading="checking">执行校验</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 校验结果 -->
    <el-card shadow="never" v-if="results.length > 0">
      <template #header>
        <span>校验结果 ({{ results.length }} 条)</span>
        <el-tag style="margin-left: 12px" :type="passRate >= 80 ? 'success' : passRate >= 50 ? 'warning' : 'danger'">
          通过率 {{ passRate }}%
        </el-tag>
      </template>

      <el-table :data="results" border stripe>
        <el-table-column prop="businessId" label="种植批次" width="180" />
        <el-table-column prop="fieldName" label="地块" width="140" />
        <el-table-column label="亩产(kg/亩)" width="110">
          <template #default="{ row }">
            <span :style="{ color: row.muYieldPass ? '' : '#e6a23c' }">{{ row.muYield }}</span>
          </template>
        </el-table-column>
        <el-table-column label="产出率(%)" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.outputRatePass ? '' : '#e6a23c' }">{{ row.outputRate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="亩产结果" width="100">
          <template #default="{ row }"><el-tag :type="row.muYieldPass ? 'success' : 'warning'" size="small">{{ row.muYieldPass ? '正常' : '异常' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="产出率结果" width="100">
          <template #default="{ row }"><el-tag :type="row.outputRatePass ? 'success' : 'warning'" size="small">{{ row.outputRatePass ? '正常' : '异常' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="综合" width="80">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small">{{ row.passed ? 'PASS' : 'FAIL' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议" minWidth="200" />
      </el-table>
    </el-card>

    <el-empty v-else description="点击「执行校验」开始产量平衡分析" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { regulationApi } from '@/api/modules/regulation'
import type { YieldBalanceResult, YieldBalanceScope } from '@/types/regulation'

interface CheckResult {
  businessId: string; fieldName: string; muYield: number; muYieldPass: boolean
  outputRate: number; outputRatePass: boolean; passed: boolean; suggestion: string
}

const checking = ref(false)
const results = ref<CheckResult[]>([])
const form = reactive({ scope: 'ALL', batchId: '' })

const passRate = computed(() => {
  if (results.value.length === 0) return 0
  return Math.round(results.value.filter((r) => r.passed).length / results.value.length * 100)
})

function mapResult(record: YieldBalanceResult): CheckResult {
  const muYield = record.items.find((item) => item.itemName.includes('亩产'))
  const outputRate = record.items.find((item) => item.itemName.includes('产出率'))
  const failedItems = record.items.filter((item) => item.result !== 'PASS')
  return {
    businessId: record.plantingBatchId || record.productBatchId || record.checkId,
    fieldName: record.checkId,
    muYield: muYield?.computedValue || 0,
    muYieldPass: !muYield || muYield.result === 'PASS',
    outputRate: outputRate?.computedValue || 0,
    outputRatePass: !outputRate || outputRate.result === 'PASS',
    passed: record.result === 'PASS',
    suggestion: failedItems.length ? `${failedItems.map((item) => item.itemName).join('、')}异常，请核对投入产出数据` : '各项指标均在正常范围内',
  }
}

async function loadResults() {
  const response = await regulationApi.getYieldBalanceResults({ page: 1, pageSize: 20 })
  results.value = response.data.records.map(mapResult)
}

async function runCheck() {
  checking.value = true
  try {
    const scopeMap: Record<string, YieldBalanceScope> = { ALL: 'FULL_CHAIN', BATCH: 'PLANTING_TO_STORAGE', FIELD: 'PLANTING_TO_STORAGE' }
    await regulationApi.checkYieldBalance({ checkScope: scopeMap[form.scope] || 'FULL_CHAIN', plantingBatchId: form.batchId || undefined })
    await loadResults()
  } finally {
    checking.value = false
  }
}

onMounted(loadResults)
</script>
