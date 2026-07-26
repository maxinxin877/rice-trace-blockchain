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
import { ref, reactive, computed } from 'vue'
import PageContainer from '@/components/common/PageContainer.vue'

interface CheckResult {
  businessId: string; fieldName: string; muYield: number; muYieldPass: boolean
  outputRate: number; outputRatePass: boolean; passed: boolean; suggestion: string
}

// 阈值
const MU_MIN = 300, MU_MAX = 700
const RATE_MIN = 55, RATE_MAX = 75

const checking = ref(false)
const results = ref<CheckResult[]>([])
const form = reactive({ scope: 'ALL', batchId: '' })

const passRate = computed(() => {
  if (results.value.length === 0) return 0
  return Math.round(results.value.filter((r) => r.passed).length / results.value.length * 100)
})

const mockResults: CheckResult[] = [
  { businessId: 'RPB202607010001', fieldName: '五常一号稻田', muYield: 650, muYieldPass: true, outputRate: 65, outputRatePass: true, passed: true, suggestion: '各项指标均在正常范围内' },
  { businessId: 'RPB202607010002', fieldName: '五常二号稻田', muYield: 650, muYieldPass: true, outputRate: 60, outputRatePass: true, passed: true, suggestion: '产出率略高于下限，建议关注原粮水分' },
  { businessId: 'RPB202607010003', fieldName: '长春水稻试验田', muYield: 280, muYieldPass: false, outputRate: 0, outputRatePass: true, passed: false, suggestion: '亩产低于300kg阈值，建议核实收割面积和称重数据' },
  { businessId: 'RPB202607010004', fieldName: '盘锦蟹田稻基地', muYield: 550, muYieldPass: true, outputRate: 0, outputRatePass: true, passed: true, suggestion: '亩产正常，尚未有加工数据' },
  { businessId: 'RPB202607010005', fieldName: '齐齐哈尔绿色稻田', muYield: 720, muYieldPass: false, outputRate: 0, outputRatePass: true, passed: false, suggestion: '亩产超过700kg上限，请核实是否存在数据录入错误' },
]

function runCheck() {
  checking.value = true
  setTimeout(() => {
    results.value = mockResults
    checking.value = false
  }, 800)
}
</script>
