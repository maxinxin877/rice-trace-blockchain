<template>
  <PageContainer title="成品批次">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增成品批次
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
      <template #actions="{ row }">
        <el-button type="success" link size="small" @click="openTrace(row)">溯源</el-button>
      </template>
    </DataTable>

    <!-- 新增弹窗 -->
    <el-dialog v-model="showForm" title="新增成品批次" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="成品批次ID" prop="productBatchId">
          <el-select v-model="form.productBatchId" placeholder="请选择加工时创建的成品批次ID" filterable style="width: 100%">
            <el-option v-for="pid in millingProductIds" :key="pid" :label="pid" :value="pid" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="如: 五常有机稻花香大米" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌名称" prop="brandName">
              <el-input v-model="form.brandName" placeholder="如: 五常御品" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="水稻品种" prop="riceVariety">
              <el-input v-model="form.riceVariety" placeholder="如: 五优稻4号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="包装规格" prop="packageSpec">
              <el-input v-model="form.packageSpec" placeholder="如: 5kg/袋" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="执行标准号" prop="standardNo">
          <el-input v-model="form.standardNo" placeholder="如: GB/T 19266" />
        </el-form-item>
        <el-form-item label="预期销售区域">
          <el-input v-model="form.expectedSaleRegion" placeholder="如: 北京、上海、广州" />
        </el-form-item>

        <el-divider content-position="left">营养成分表</el-divider>
        <NutritionEditor v-model="form.nutritionFacts" />
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 全链路溯源弹窗 -->
    <el-dialog v-model="showTrace" title="产品全链路溯源" width="750px">
      <div class="trace-chain" v-if="traceData">
        <div class="trace-node" v-for="(node, idx) in traceData" :key="idx">
          <div class="trace-connector" v-if="idx > 0">
            <div class="connector-line"></div>
            <el-icon class="connector-arrow"><ArrowDown /></el-icon>
          </div>
          <div class="node-card" :style="{ borderLeftColor: node.color }">
            <div class="node-header">
              <span class="node-step" :style="{ backgroundColor: node.color }">{{ node.step }}</span>
              <span class="node-title">{{ node.title }}</span>
            </div>
            <div class="node-body">
              <div class="node-row" v-for="(val, key) in node.fields" :key="key">
                <span class="node-key">{{ key }}</span>
                <span class="node-val">{{ val || '-' }}</span>
              </div>
            </div>
            <div class="node-id">{{ node.id }}</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import NutritionEditor from '@/components/rice/NutritionEditor.vue'
import { productBatchApi } from '@/api/modules/productBatch'
import { millingBatchApi } from '@/api/modules/millingBatch'
import { useTable } from '@/composables/useTable'
import type { RiceProductBatch, NutritionFacts, TraceResult } from '@/types/productBatch'

// 溯源数据
interface TraceNode {
  step: string; title: string; id: string; color: string
  fields: Record<string, string>
}

const showTrace = ref(false)
const traceData = ref<TraceNode[] | null>(null)

function openTrace(row: RiceProductBatch) {
  productBatchApi.trace(row.productBatchId).then((res) => {
    if (res.code === 200) {
      traceData.value = buildTraceNodes(res.data)
      showTrace.value = true
    }
  }).catch(() => {
    ElMessage.error('溯源查询失败')
  })
}

function buildTraceNodes(d: TraceResult): TraceNode[] {
  const nodes: TraceNode[] = []
  if (d.field) {
    nodes.push({ step: '1', title: '地块档案', id: String(d.field.fieldId ?? '-'), color: '#409EFF', fields: {
      '地块名称': String(d.field.fieldName ?? '-'),
      '种植户': String(d.field.farmerName ?? '-'),
      '所在地区': [d.field.province, d.field.city, d.field.district].filter(Boolean).join(' '),
      '面积(亩)': String(d.field.areaMu ?? '-'),
    }})
  }
  if (d.plantingBatch) {
    nodes.push({ step: '2', title: '种植批次', id: String(d.plantingBatch.plantingBatchId ?? '-'), color: '#67C23A', fields: {
      '水稻品种': String(d.plantingBatch.riceVariety ?? '-'),
      '种子来源': String(d.plantingBatch.seedSource ?? '-'),
      '播种日期': String(d.plantingBatch.sowingDate ?? '-'),
    }})
  }
  if (d.storageReceipt) {
    nodes.push({ step: '3', title: '收储入库', id: String(d.storageReceipt.storageReceiptId ?? '-'), color: '#E6A23C', fields: {
      '原粮批次': String(d.storageReceipt.grainBatchId ?? '-'),
      '入仓编号': String(d.storageReceipt.warehouseCode ?? '-'),
      '湿谷重量(kg)': String(d.storageReceipt.wetGrainWeightKg ?? '-'),
      '等级': String(d.storageReceipt.grainGrade ?? '-'),
    }})
  }
  if (d.millingBatch) {
    nodes.push({ step: '4', title: '碾米加工', id: String(d.millingBatch.millingBatchId ?? '-'), color: '#00BCD4', fields: {
      '原粮批次': String(d.millingBatch.grainBatchId ?? '-'),
      '精米产出量(kg)': d.millingBatch.riceOutputWeightKg != null ? String(d.millingBatch.riceOutputWeightKg) : '-',
      '产出率(%)': d.millingBatch.yieldRate != null ? String(d.millingBatch.yieldRate) : '-',
    }})
  }
  if (d.productBatch) {
    nodes.push({ step: '5', title: '成品批次', id: d.productBatch.productBatchId, color: '#9C27B0', fields: {
      '产品名称': String(d.productBatch.productName ?? '-'),
      '品牌': String(d.productBatch.brandName ?? '-'),
      '规格': String(d.productBatch.packageSpec ?? '-'),
      '标准号': String(d.productBatch.standardNo ?? '-'),
    }})
  }
  if (d.traceCodes && d.traceCodes.length) {
    const activated = d.traceCodes.filter((t) => t.status === 'ACTIVATED').length
    nodes.push({ step: '6', title: '防伪码', id: `${d.traceCodes.length} 枚`, color: '#F56C6C', fields: {
      '已生成': `${d.traceCodes.length} 枚`,
      '已激活': `${activated} 枚`,
    }})
  }
  return nodes
}

const searchItems: SearchItem[] = [
  { prop: 'productName', label: '产品名称', type: 'input', placeholder: '输入产品名称' },
  { prop: 'brandName', label: '品牌', type: 'input', placeholder: '输入品牌名称' },
  { prop: 'riceVariety', label: '水稻品种', type: 'input', placeholder: '输入品种' },
]

const columns: TableColumn[] = [
  { prop: 'productBatchId', label: '成品批次ID', width: 180 },
  { prop: 'productName', label: '产品名称', minWidth: 180 },
  { prop: 'brandName', label: '品牌', width: 120 },
  { prop: 'riceVariety', label: '水稻品种', width: 130 },
  { prop: 'packageSpec', label: '包装规格', width: 110 },
  { prop: 'standardNo', label: '执行标准', width: 130 },
  { prop: 'expectedSaleRegion', label: '预期销售区域', minWidth: 150 },
  { prop: 'status', label: '状态', width: 90 },
]

const fetchFn = (params: Record<string, unknown>) =>
  productBatchApi.getList(params) as Promise<{ data: { records: RiceProductBatch[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RiceProductBatch>(fetchFn)

// 碾米加工中已定义的成品批次ID（下拉选项）
const millingProductIds = ref<string[]>([])

onMounted(async () => {
  loadData()
  const res = await millingBatchApi.getList({ pageSize: 100 })
  if (res.code === 200) {
    millingProductIds.value = [...new Set(res.data.records.map((m) => m.productBatchId).filter(Boolean))]
  }
})

// 表单
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  productBatchId: '',
  productName: '',
  brandName: '',
  riceVariety: '',
  packageSpec: '',
  standardNo: '',
  expectedSaleRegion: '',
  nutritionFacts: {} as NutritionFacts,
})

const rules: FormRules = {
  productBatchId: [{ required: true, message: '请输入批次ID', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  brandName: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }],
  riceVariety: [{ required: true, message: '请输入水稻品种', trigger: 'blur' }],
  packageSpec: [{ required: true, message: '请输入包装规格', trigger: 'blur' }],
  standardNo: [{ required: true, message: '请输入执行标准号', trigger: 'blur' }],
}

function openCreate() {
  form.productBatchId = ''
  form.productName = ''
  form.brandName = ''
  form.riceVariety = ''
  form.packageSpec = ''
  form.standardNo = ''
  form.expectedSaleRegion = ''
  form.nutritionFacts = {}
  showForm.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await productBatchApi.create({
      productBatchId: form.productBatchId,
      productName: form.productName,
      brandName: form.brandName,
      riceVariety: form.riceVariety,
      packageSpec: form.packageSpec,
      standardNo: form.standardNo,
      expectedSaleRegion: form.expectedSaleRegion,
      nutritionFacts: form.nutritionFacts,
    })
    ElMessage.success('成品批次创建成功')
    showForm.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* 溯源链路 */
.trace-chain {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.trace-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2px 0;
}

.connector-line {
  width: 2px;
  height: 24px;
  background: #d0d5dd;
}

.connector-arrow {
  color: #d0d5dd;
  font-size: 14px;
  margin-top: -2px;
}

.node-card {
  width: 100%;
  max-width: 600px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-left: 4px solid #409EFF;
  border-radius: 8px;
  padding: 14px 18px;
  transition: box-shadow 0.2s;
}

.node-card:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.node-step {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.node-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.node-body {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 24px;
}

.node-row {
  font-size: 13px;
}

.node-key {
  color: #86909c;
  margin-right: 4px;
}

.node-key::after {
  content: ':';
}

.node-val {
  color: #1d2129;
}

.node-id {
  margin-top: 8px;
  font-size: 11px;
  color: #c9cdd4;
  font-family: monospace;
}
</style>
