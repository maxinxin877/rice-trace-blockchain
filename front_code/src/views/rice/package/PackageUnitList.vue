<template>
  <PageContainer title="包装编码（箱 / 托盘）">
    <template #actions>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 创建箱码/托盘码
      </el-button>
    </template>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="pkg-tip"
      title="编码层级：托盘码 → 箱码 → 单品码 → 批次 → 地块。扫码时无论扫到哪一层，都能展开到单品并追溯到上游批次。"
    />

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
      <template #unitCode="{ row }">
        <el-link type="primary" @click="showDetail(row.unitCode)">{{ row.unitCode }}</el-link>
      </template>
      <template #unitLevel="{ row }">
        <el-tag size="small" :type="row.unitLevel === 'PALLET' ? 'warning' : 'success'" effect="plain">
          {{ LEVEL_MAP[row.unitLevel] || row.unitLevel }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="showDetail(row.unitCode)">展开</el-button>
      </template>
    </DataTable>

    <!-- 创建 -->
    <el-dialog v-model="createVisible" title="创建箱码 / 托盘码" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="层级" prop="unitLevel">
          <el-radio-group v-model="form.unitLevel">
            <el-radio-button value="BOX">箱码（绑定单品码）</el-radio-button>
            <el-radio-button value="PALLET">托盘码（绑定箱码）</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.unitLevel === 'BOX'" label="成品批次" prop="productBatchId">
          <el-input v-model="form.productBatchId" placeholder="如：PRD202610010001" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="包装规格">
              <el-input v-model="form.packageSpec" placeholder="如：5kg/袋" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售区域">
              <el-input v-model="form.expectedSaleRegion" placeholder="如：黑龙江省哈尔滨市" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="form.unitLevel === 'BOX' ? '单品码清单' : '箱码清单'" prop="childCodesText">
          <el-input
            v-model="form.childCodesText"
            type="textarea"
            :rows="6"
            :placeholder="form.unitLevel === 'BOX'
              ? '每行一个单品码，也支持逗号分隔'
              : '每行一个已创建的箱码'"
          />
        </el-form-item>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="校验规则：子码必须存在、同属一个成品批次、未被其他箱/托盘绑定；已停用（召回冻结）的单品码禁止装箱。"
        />
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建并绑定</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="detail?.unit.unitCode || '包装单元'" size="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="层级">{{ LEVEL_MAP[detail.unit.unitLevel] }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.unit.status }}</el-descriptions-item>
          <el-descriptions-item label="成品批次">{{ detail.unit.productBatchId }}</el-descriptions-item>
          <el-descriptions-item label="包装规格">{{ detail.unit.packageSpec || '-' }}</el-descriptions-item>
          <el-descriptions-item label="子码数量">{{ detail.unit.childCount }}</el-descriptions-item>
          <el-descriptions-item label="折算单品">{{ detail.unit.totalQuantity }}</el-descriptions-item>
          <el-descriptions-item label="销售区域" :span="2">
            {{ detail.unit.expectedSaleRegion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="上级码" :span="2">
            {{ detail.parent?.unitCode || detail.unit.parentCode || '无（顶层）' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="pkg-section">子码清单（{{ detail.children.length }}）</h4>
        <el-table :data="detail.children" border size="small" max-height="260">
          <el-table-column prop="childCode" label="子码" min-width="170" />
          <el-table-column prop="childLevel" label="层级" width="100" align="center">
            <template #default="{ row }">{{ row.childLevel === 'BOX' ? '箱码' : '单品码' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" align="center" />
          <el-table-column prop="scanCount" label="扫码次数" width="90" align="center" />
        </el-table>

        <h4 class="pkg-section">展开后的单品码（{{ detail.traceCodes.length }}）</h4>
        <div class="code-list">
          <el-tag v-for="code in detail.traceCodes.slice(0, 80)" :key="code" size="small" class="code-tag">
            {{ code }}
          </el-tag>
          <span v-if="detail.traceCodes.length > 80" class="code-more">
            等共 {{ detail.traceCodes.length }} 个
          </span>
        </div>
      </template>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import { packageApi } from '@/api/modules/package'
import { useTable } from '@/composables/useTable'
import type { PackageDetail, PackageUnit } from '@/types/package'
import { UNIT_LEVEL_MAP } from '@/types/package'

const LEVEL_MAP = UNIT_LEVEL_MAP

const searchItems: SearchItem[] = [
  { prop: 'unitCode', label: '码值', type: 'input', placeholder: '箱码 / 托盘码' },
  { prop: 'productBatchId', label: '成品批次', type: 'input', placeholder: '成品批次ID' },
  {
    prop: 'unitLevel',
    label: '层级',
    type: 'select',
    options: [
      { value: 'BOX', label: '箱码' },
      { value: 'PALLET', label: '托盘码' },
    ],
  },
]

const columns: TableColumn[] = [
  { prop: 'unitCode', label: '码值', width: 190, slot: 'unitCode' },
  { prop: 'unitLevel', label: '层级', width: 100, slot: 'unitLevel' },
  { prop: 'productBatchId', label: '成品批次', minWidth: 160 },
  { prop: 'packageSpec', label: '规格', width: 100, align: 'center' },
  { prop: 'childCount', label: '子码数', width: 90, align: 'center' },
  { prop: 'totalQuantity', label: '折算单品', width: 100, align: 'center' },
  { prop: 'expectedSaleRegion', label: '销售区域', minWidth: 150 },
  { prop: 'status', label: '状态', width: 100, align: 'center' },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const fetchFn = (params: Record<string, unknown>) =>
  packageApi.getList(params) as Promise<{ data: { records: PackageUnit[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } =
  useTable<PackageUnit>(fetchFn)

onMounted(() => loadData())

// ---------------- 创建 ----------------
const createVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  unitLevel: 'BOX' as 'BOX' | 'PALLET',
  productBatchId: '',
  packageSpec: '',
  expectedSaleRegion: '',
  childCodesText: '',
})

const rules: FormRules = {
  unitLevel: [{ required: true, message: '请选择层级', trigger: 'change' }],
  childCodesText: [{ required: true, message: '请填写子码清单', trigger: 'blur' }],
}

function openCreate() {
  form.productBatchId = ''
  form.packageSpec = ''
  form.expectedSaleRegion = ''
  form.childCodesText = ''
  createVisible.value = true
}

async function submitCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const childCodes = form.childCodesText
    .split(/[\s,，;；]+/)
    .map((s) => s.trim())
    .filter(Boolean)
  if (childCodes.length === 0) {
    ElMessage.warning('请至少填写一个子码')
    return
  }
  if (form.unitLevel === 'BOX' && !form.productBatchId.trim()) {
    ElMessage.warning('箱码必须指定成品批次')
    return
  }
  submitting.value = true
  try {
    const res = await packageApi.create({
      unitLevel: form.unitLevel,
      productBatchId: form.productBatchId.trim() || undefined,
      childCodes,
      packageSpec: form.packageSpec,
      expectedSaleRegion: form.expectedSaleRegion,
    })
    ElMessage.success(`已创建 ${res.data.unitCode}，绑定 ${childCodes.length} 个子码`)
    createVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---------------- 详情 ----------------
const detailVisible = ref(false)
const detail = ref<PackageDetail | null>(null)

async function showDetail(unitCode: string) {
  const res = await packageApi.detail(unitCode)
  detail.value = res.data
  detailVisible.value = true
}
</script>

<style scoped>
.pkg-tip {
  margin-bottom: 14px;
}

.pkg-section {
  margin: 18px 0 10px;
  font-size: 14px;
  color: #1f2937;
}

.code-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.code-tag {
  font-family: Consolas, monospace;
}

.code-more {
  font-size: 12px;
  color: #909399;
  align-self: center;
}
</style>
