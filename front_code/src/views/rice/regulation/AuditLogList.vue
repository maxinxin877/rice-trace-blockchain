<template>
  <PageContainer title="审计日志">
    <SearchForm :items="searchItems" @search="onSearch" @reset="onReset" />

    <DataTable :columns="columns" :data="data" :loading="loading" :total="total" :page="page" :pageSize="pageSize" @page-change="onPageChange">
      <template #operationType="{ row }">{{ row.operationType === 'CREATE' ? '创建' : row.operationType === 'UPDATE' ? '修改' : row.operationType }}</template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
      </template>
    </DataTable>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="审计日志详情" width="650px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="审计ID">{{ currentLog?.auditId }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog?.operationType === 'CREATE' ? '创建' : currentLog?.operationType === 'UPDATE' ? '修改' : currentLog?.operationType }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ currentLog?.businessType }}</el-descriptions-item>
        <el-descriptions-item label="业务ID">{{ currentLog?.businessId }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog?.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog?.operationTime }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ currentLog?.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="修改原因">{{ currentLog?.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="修改前哈希" :span="2">{{ currentLog?.beforeHash || '(新建)' }}</el-descriptions-item>
        <el-descriptions-item label="修改后哈希" :span="2">{{ currentLog?.afterHash || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链上交易ID" :span="2">{{ currentLog?.txId || '未上链' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageContainer from '@/components/common/PageContainer.vue'
import SearchForm from '@/components/common/SearchForm.vue'
import type { SearchItem } from '@/components/common/SearchForm.vue'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import { useTable } from '@/composables/useTable'
import { regulationApi } from '@/api/modules/regulation'
import type { AuditLogRecord } from '@/types/regulation'

const searchItems: SearchItem[] = [
  { prop: 'businessType', label: '业务类型', type: 'input', placeholder: '如: FIELD' },
  { prop: 'businessId', label: '业务ID', type: 'input', placeholder: '输入业务ID' },
  { prop: 'operatorId', label: '操作人ID', type: 'input', placeholder: '输入操作人ID' },
  { prop: 'operationType', label: '操作类型', type: 'select', options: ['CREATE', 'UPDATE', 'DELETE', 'VERIFY', 'HANDLE'].map((value) => ({ value, label: value })) },
]

const columns: TableColumn[] = [
  { prop: 'auditId', label: '审计ID', width: 110 },
  { prop: 'businessType', label: '业务类型', width: 120 },
  { prop: 'businessId', label: '业务ID', width: 180 },
  { prop: 'operationType', label: '操作', width: 60, slot: 'operationType' },
  { prop: 'operatorName', label: '操作人', width: 120 },
  { prop: 'reason', label: '原因', minWidth: 150 },
  { prop: 'operationTime', label: '操作时间', width: 170 },
]

const fetchFn = async (params: Record<string, unknown>) => {
  const response = await regulationApi.getAuditLogs(params)
  return { data: response.data }
}

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<AuditLogRecord>(fetchFn)
onMounted(() => loadData())

const showDetailDialog = ref(false)
const currentLog = ref<AuditLogRecord | null>(null)

function showDetail(row: AuditLogRecord) { currentLog.value = row; showDetailDialog.value = true }
</script>
