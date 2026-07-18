<template>
  <PageContainer title="地块档案">
    <template #actions>
      <el-button type="primary" @click="$router.push('/rice/fields/create')">
        <el-icon><Plus /></el-icon> 新增地块
      </el-button>
    </template>

    <!-- 搜索表单 -->
    <SearchForm :items="searchItems" @search="onSearch" @reset="onReset" />

    <!-- 数据表格 -->
    <DataTable
      :columns="columns"
      :data="data"
      :loading="loading"
      :total="total"
      :page="page"
      :pageSize="pageSize"
      @page-change="onPageChange"
    >
      <template #chainStatus="{ row }">
        <StatusTag type="chain" :value="row.chainStatus" />
      </template>
      <template #areaMu="{ row }">
        {{ formatArea(row.areaMu) }}
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="viewDetail(row.fieldId)">详情</el-button>
        <el-button type="primary" link size="small" @click="editField(row.fieldId)">编辑</el-button>
      </template>
    </DataTable>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { fieldApi } from '@/api/modules/field'
import { useTable } from '@/composables/useTable'
import { formatArea } from '@/utils/format'
import { CHAIN_STATUS_MAP } from '@/utils/constants'
import type { RiceField } from '@/types/field'

const router = useRouter()

const searchItems: SearchItem[] = [
  { prop: 'fieldCode', label: '地块编号', type: 'input', placeholder: '输入地块编号' },
  { prop: 'fieldName', label: '地块名称', type: 'input', placeholder: '输入地块名称' },
  { prop: 'farmerName', label: '种植户', type: 'input', placeholder: '输入种植户名称' },
  { prop: 'chainStatus', label: '链上状态', type: 'select', options: Object.entries(CHAIN_STATUS_MAP).map(([value, item]) => ({ value, label: item.label })) },
]

const columns: TableColumn[] = [
  { prop: 'fieldCode', label: '地块编号', width: 130 },
  { prop: 'fieldName', label: '地块名称', width: 160 },
  { prop: 'farmerName', label: '种植户', width: 100 },
  { prop: 'province', label: '省', width: 100 },
  { prop: 'city', label: '市', width: 100 },
  { prop: 'district', label: '区/县', width: 100 },
  { prop: 'areaMu', label: '面积', width: 100, slot: 'areaMu' },
  { prop: 'soilType', label: '土壤类型', width: 90 },
  { prop: 'chainStatus', label: '链上状态', width: 90, slot: 'chainStatus' },
]

const fetchFn = (params: Record<string, unknown>) =>
  fieldApi.getList(params as Record<string, unknown>) as Promise<{ data: { records: RiceField[]; total: number } }>

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<RiceField>(fetchFn)

onMounted(() => {
  loadData()
})

function viewDetail(id: string) {
  router.push(`/rice/fields/detail/${id}`)
}

function editField(id: string) {
  router.push(`/rice/fields/${id}/edit`)
}
</script>
