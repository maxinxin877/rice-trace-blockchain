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

interface AuditLog {
  auditId: string; businessType: string; businessId: string; operationType: string
  operatorId: string; operatorName: string; beforeHash: string | null; afterHash: string | null
  reason: string | null; ip: string | null; operationTime: string; txId: string | null
}

const mockLogs: AuditLog[] = [
  { auditId: 'AUDIT001', businessType: 'FIELD', businessId: 'FIELD202607010001', operationType: 'CREATE', operatorId: 'ADMIN001', operatorName: '管理员', beforeHash: null, afterHash: 'sha256:abc123', reason: null, ip: '192.168.1.100', operationTime: '2026-07-01 08:00:00', txId: '0xabc001' },
  { auditId: 'AUDIT002', businessType: 'FIELD', businessId: 'FIELD202607010001', operationType: 'UPDATE', operatorId: 'ADMIN001', operatorName: '管理员', beforeHash: 'sha256:abc123', afterHash: 'sha256:def456', reason: '修正面积数据', ip: '192.168.1.100', operationTime: '2026-07-05 14:00:00', txId: '0xabc002' },
  { auditId: 'AUDIT003', businessType: 'PLANTING_BATCH', businessId: 'RPB202607010001', operationType: 'CREATE', operatorId: 'ADMIN001', operatorName: '管理员', beforeHash: null, afterHash: 'sha256:plant001', reason: null, ip: '192.168.1.101', operationTime: '2026-07-01 08:30:00', txId: '0xchain001' },
  { auditId: 'AUDIT004', businessType: 'FARMING_LOG', businessId: 'FLOG202607010003', operationType: 'CREATE', operatorId: 'FARMER001', operatorName: '张三丰', beforeHash: null, afterHash: 'sha256:flog003', reason: null, ip: '10.0.0.50', operationTime: '2026-07-01 06:30:00', txId: null },
  { auditId: 'AUDIT005', businessType: 'STORAGE_RECEIPT', businessId: 'SREC202607010001', operationType: 'CREATE', operatorId: 'WH_STAFF001', operatorName: '仓储员李仓库', beforeHash: null, afterHash: 'sha256:srec001', reason: null, ip: '10.0.1.20', operationTime: '2026-09-20 18:30:00', txId: '0xsrec001' },
  { auditId: 'AUDIT006', businessType: 'MILLING_BATCH', businessId: 'MB202607010002', operationType: 'UPDATE', operatorId: 'FACTORY_STAFF001', operatorName: '加工员王工厂', beforeHash: 'sha256:oldhash', afterHash: 'sha256:newhash', reason: '修正产出率数据', ip: '10.0.2.10', operationTime: '2026-10-04 10:00:00', txId: null },
]

const searchItems: SearchItem[] = [
  { prop: 'businessType', label: '业务类型', type: 'input', placeholder: '如: FIELD' },
  { prop: 'businessId', label: '业务ID', type: 'input', placeholder: '输入业务ID' },
  { prop: 'operatorName', label: '操作人', type: 'input', placeholder: '输入操作人' },
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
  let list = [...mockLogs]
  if (params.businessType) list = list.filter((l) => l.businessType.includes(params.businessType as string))
  if (params.businessId) list = list.filter((l) => l.businessId.includes(params.businessId as string))
  if (params.operatorName) list = list.filter((l) => l.operatorName.includes(params.operatorName as string))
  const p = (params.page as number) || 1; const ps = (params.pageSize as number) || 10
  return { data: { records: list.slice((p - 1) * ps, (p - 1) * ps + ps), total: list.length } }
}

const { loading, data, total, page, pageSize, loadData, onSearch, onReset, onPageChange } = useTable<AuditLog>(fetchFn)
onMounted(() => loadData())

const showDetailDialog = ref(false)
const currentLog = ref<AuditLog | null>(null)

function showDetail(row: AuditLog) { currentLog.value = row; showDetailDialog.value = true }
</script>
