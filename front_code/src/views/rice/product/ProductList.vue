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
        <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
        <el-button type="success" link size="small" @click="openTrace(row)">溯源</el-button>
      </template>
    </DataTable>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showForm" :title="isEdit ? '编辑成品批次' : '新增成品批次'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item v-if="!isEdit" label="成品批次ID" prop="productBatchId">
          <el-input v-model="form.productBatchId" placeholder="如: PROD20261001001" />
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
import { useTable } from '@/composables/useTable'
import type { RiceProductBatch, NutritionFacts } from '@/types/productBatch'

// 溯源数据
interface TraceNode {
  step: string; title: string; id: string; color: string
  fields: Record<string, string>
}

const showTrace = ref(false)
const traceData = ref<TraceNode[] | null>(null)

// Mock 溯源链路（按 productBatchId 返回对应链路）
const mockTraces: Record<string, TraceNode[]> = {
  PROD20261001001: [
    { step: '1', title: '地块档案', id: 'FIELD202607010001', color: '#409EFF', fields: { '地块名称': '五常一号稻田', '种植户': '张三丰', '面积': '150.50 亩', '土壤': '黑土', '坐标': '127.5678, 45.1234' } },
    { step: '2', title: '种植批次', id: 'RPB202607010001', color: '#67C23A', fields: { '水稻品种': '五优稻4号', '种子来源': '五常市种子公司', '播种日期': '2026-05-01', '收割日期': '2026-09-20', '有机认证': '是' } },
    { step: '3', title: '收储入库', id: 'SREC202607010001', color: '#E6A23C', fields: { '原粮批次': 'GRAIN20260920001', '入仓编号': 'WC-01-001', '湿谷重量': '97,500 kg', '水分': '25.50%', '等级': '一等' } },
    { step: '4', title: '入库质检', id: 'QTEST202607010001', color: '#FF9800', fields: { '检测机构': '黑龙江省粮油质量检测中心', '综合结论': '合格', '水分': '14.50%', '出糙率': '79%', '整精米率': '58%' } },
    { step: '5', title: '碾米加工', id: 'MB202607010001', color: '#00BCD4', fields: { '原粮出库量': '50,000 kg', '精米产出量': '32,500 kg', '产出率': '65%', '脱壳': '胶辊砻谷机', '包装': '真空包装' } },
    { step: '6', title: '成品批次', id: 'PROD20261001001', color: '#9C27B0', fields: { '产品名称': '五常有机稻花香大米', '品牌': '五常御品', '规格': '5kg/袋', '标准号': 'GB/T 19266' } },
    { step: '7', title: '防伪码', id: 'RC20261001...', color: '#F56C6C', fields: { '已生成': '100 枚', '已激活': '80 枚', '累计扫码': '128 次', '风险等级': '低' } },
  ],
  PROD20261001002: [
    { step: '1', title: '地块档案', id: 'FIELD202607010001', color: '#409EFF', fields: { '地块名称': '五常一号稻田', '种植户': '张三丰', '面积': '150.50 亩' } },
    { step: '2', title: '种植批次', id: 'RPB202607010001', color: '#67C23A', fields: { '水稻品种': '五优稻4号', '播种日期': '2026-05-01' } },
    { step: '3', title: '收储入库', id: 'SREC202607010001', color: '#E6A23C', fields: { '原粮批次': 'GRAIN20260920001', '等级': '一等' } },
    { step: '4', title: '碾米加工', id: 'MB202607010002', color: '#00BCD4', fields: { '原粮出库量': '47,500 kg', '精米产出量': '28,500 kg', '产出率': '60%' } },
    { step: '5', title: '成品批次', id: 'PROD20261001002', color: '#9C27B0', fields: { '产品名称': '五常大米（优质）', '品牌': '五常御品', '规格': '10kg/袋' } },
  ],
  PROD20261001003: [
    { step: '1', title: '地块档案', id: 'FIELD202607010002', color: '#409EFF', fields: { '地块名称': '五常二号稻田', '种植户': '李四喜', '面积': '200 亩' } },
    { step: '2', title: '种植批次', id: 'RPB202607010002', color: '#67C23A', fields: { '水稻品种': '稻花香2号', '播种日期': '2026-05-10' } },
    { step: '3', title: '收储入库', id: 'SREC202607010002', color: '#E6A23C', fields: { '原粮批次': 'GRAIN20260925001', '等级': '一等' } },
    { step: '4', title: '碾米加工', id: 'MB202607010003', color: '#00BCD4', fields: { '工厂': 'FACTORY002', '四道抛光': 'AI色选机' } },
    { step: '5', title: '成品批次', id: 'PROD20261001003', color: '#9C27B0', fields: { '产品名称': '稻花香2号精米', '品牌': '龙江香米', '规格': '2.5kg/盒' } },
  ],
}

function openTrace(row: RiceProductBatch) {
  traceData.value = mockTraces[row.productBatchId] || generateDefaultTrace(row)
  showTrace.value = true
}

function generateDefaultTrace(row: RiceProductBatch): TraceNode[] {
  return [
    { step: '1', title: '地块档案', id: '—', color: '#409EFF', fields: { '提示': '暂无关联地块数据' } },
    { step: '2', title: '种植批次', id: '—', color: '#67C23A', fields: { '提示': '暂无关联种植批次' } },
    { step: '3', title: '收储入库', id: '—', color: '#E6A23C', fields: { '提示': '暂无关联入库数据' } },
    { step: '4', title: '碾米加工', id: '—', color: '#00BCD4', fields: { '提示': '暂无关联加工数据' } },
    { step: '5', title: '成品批次', id: row.productBatchId, color: '#9C27B0', fields: { '产品名称': row.productName, '品牌': row.brandName, '规格': row.packageSpec, '标准号': row.standardNo } },
  ]
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

onMounted(() => loadData())

// 表单
const showForm = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref('')
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
  isEdit.value = false
  editingId.value = ''
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

function openEdit(row: RiceProductBatch) {
  isEdit.value = true
  editingId.value = row.productBatchId
  form.productBatchId = row.productBatchId
  form.productName = row.productName
  form.brandName = row.brandName
  form.riceVariety = row.riceVariety
  form.packageSpec = row.packageSpec
  form.standardNo = row.standardNo
  form.expectedSaleRegion = row.expectedSaleRegion || ''
  form.nutritionFacts = row.nutritionFacts || {}
  showForm.value = true
}

async function submitForm() {
  if (!formRef.value) return
  // 编辑时不需要校验 productBatchId
  const rulesToValidate = isEdit.value ? { ...rules, productBatchId: [] } : rules
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await productBatchApi.update(editingId.value, {
        productName: form.productName,
        brandName: form.brandName,
        riceVariety: form.riceVariety,
        packageSpec: form.packageSpec,
        standardNo: form.standardNo,
        expectedSaleRegion: form.expectedSaleRegion,
        nutritionFacts: form.nutritionFacts,
      })
      ElMessage.success('成品批次更新成功')
    } else {
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
    }
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
