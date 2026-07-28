<template>
  <PageContainer title="链上存证与核验">
    <template #actions><el-button :loading="loading" @click="loadData"><el-icon><Refresh /></el-icon>刷新存证</el-button></template>
    <el-form class="query" :model="query" inline>
      <el-form-item label="业务类型"><el-select v-model="query.businessType" style="width:180px"><el-option v-for="item in businessTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="业务 ID"><el-input v-model="query.businessId" style="width:260px" /></el-form-item>
      <el-form-item><el-button type="primary" @click="searchProof">查询存证</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="proofs" border stripe>
      <el-table-column prop="businessType" label="业务类型" width="150" />
      <el-table-column prop="businessId" label="业务 ID" min-width="190" />
      <el-table-column label="上链状态" width="100"><template #default="{ row }"><StatusTag type="chain" :value="row.chainStatus" /></template></el-table-column>
      <el-table-column prop="blockHeight" label="区块高度" width="110" />
      <el-table-column prop="chainTime" label="上链时间" width="170" />
      <el-table-column label="核验" width="90"><template #default="{ row }"><el-tag :type="row.verified ? 'success' : 'danger'">{{ row.verified ? '一致' : '不一致' }}</el-tag></template></el-table-column>
      <el-table-column label="交易哈希" min-width="170"><template #default="{ row }"><span class="hash">{{ shortHash(row.txId) }}</span></template></el-table-column>
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button type="primary" link @click="openDetail(row)">详情</el-button><el-button type="success" link :loading="verifyingId===row.businessId" @click="verify(row)">核验</el-button></template></el-table-column>
    </el-table>
    <el-drawer v-model="showDetail" title="链上存证详情" size="640px">
      <template v-if="currentProof">
        <div :class="['banner',currentProof.verified?'ok':'bad']"><el-icon><CircleCheck v-if="currentProof.verified" /><WarningFilled v-else /></el-icon><div><strong>{{ currentProof.verified?'链上数据与当前数据一致':'链上数据核验不一致' }}</strong><p>{{ currentProof.verified?'数据完整性校验通过，可作为可信溯源凭证。':'当前摘要与链上摘要不同，应生成高风险预警。' }}</p></div></div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="业务对象">{{ currentProof.businessType }} / {{ currentProof.businessId }}</el-descriptions-item>
          <el-descriptions-item label="交易 ID"><span class="hash break">{{ currentProof.txId }}</span></el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ currentProof.blockHeight }}</el-descriptions-item>
          <el-descriptions-item label="上链时间">{{ currentProof.chainTime }}</el-descriptions-item>
          <el-descriptions-item label="链上摘要"><span class="hash break">{{ currentProof.chainHash }}</span></el-descriptions-item>
          <el-descriptions-item label="当前摘要"><span class="hash break">{{ currentProof.currentHash }}</span></el-descriptions-item>
          <el-descriptions-item label="文件指纹"><div v-for="hash in currentProof.fileHashes" :key="hash" class="hash break">{{ hash }}</div><span v-if="!currentProof.fileHashes.length">无附件</span></el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { traceabilityApi } from '@/api/modules/traceability'
import type { ChainProofRecord } from '@/types/traceability'

const businessTypes=[{label:'防伪码',value:'TRACE_CODE'},{label:'成品批次',value:'PRODUCT_BATCH'},{label:'地块',value:'FIELD'},{label:'种植批次',value:'PLANTING_BATCH'},{label:'入库单',value:'STORAGE_RECEIPT'},{label:'风险预警',value:'RISK_WARNING'}]
const query=reactive({businessType:'TRACE_CODE',businessId:'RC20260718000001'})
const loading=ref(false), proofs=ref<ChainProofRecord[]>([]), showDetail=ref(false), currentProof=ref<ChainProofRecord>(), verifyingId=ref('')
function shortHash(value:string){return value.length>22?`${value.slice(0,12)}…${value.slice(-8)}`:value}
async function loadData(){loading.value=true;try{proofs.value=(await traceabilityApi.getChainProofs()).data}finally{loading.value=false}}
async function searchProof(){const proof=(await traceabilityApi.getChainProof(query.businessType,query.businessId)).data;if(!proof)return ElMessage.warning('未找到该业务对象的链上存证');openDetail(proof)}
function openDetail(row:ChainProofRecord){currentProof.value=row;showDetail.value=true}
async function verify(row:ChainProofRecord){verifyingId.value=row.businessId;try{const proof=(await traceabilityApi.verifyChainProof(row.businessType,row.businessId)).data;if(proof){openDetail(proof);proof.verified?ElMessage.success('链上核验通过'):ElMessage.error('链上核验失败，已标记风险')}}finally{verifyingId.value=''}}
onMounted(loadData)
</script>

<style scoped>
.query{padding:14px 16px 6px;margin-bottom:16px;background:#f7f8fa;border-radius:8px}.hash{font-family:Consolas,monospace;color:#4e5969}.break{overflow-wrap:anywhere}.banner{display:flex;gap:14px;padding:18px;margin-bottom:18px;border-radius:10px}.banner .el-icon{font-size:32px}.banner strong{display:block;margin-bottom:5px}.banner p{margin:0;font-size:13px}.ok{color:#176b3a;background:#ecf8f0}.bad{color:#a22b2b;background:#fff0f0}
</style>
