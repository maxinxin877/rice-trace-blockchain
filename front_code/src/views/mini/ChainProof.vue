<template>
  <MobileShell title="链上核验" subtitle="区块链不可篡改存证" back>
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-empty v-else-if="!proof" description="未找到该产品的链上存证" />
    <template v-else>
      <section :class="['result',proof.verified?'ok':'bad']"><el-icon><CircleCheckFilled v-if="proof.verified" /><WarningFilled v-else /></el-icon><h2>{{ proof.verified?'核验通过':'核验异常' }}</h2><p>{{ proof.verified?'当前数据摘要与链上存证完全一致':'当前数据摘要与链上摘要不一致，请谨慎购买' }}</p></section>
      <section class="chain-card">
        <div class="chain-title"><span class="block">BLOCK</span><div><strong>联盟链存证凭证</strong><p>FISCO BCOS / Hyperledger Fabric 兼容展示</p></div></div>
        <dl><dt>业务对象</dt><dd>{{ proof.businessType }} / {{ proof.businessId }}</dd><dt>区块高度</dt><dd>{{ proof.blockHeight }}</dd><dt>上链时间</dt><dd>{{ proof.chainTime }}</dd><dt>交易 ID</dt><dd class="hash">{{ proof.txId }}</dd><dt>链上摘要</dt><dd class="hash">{{ proof.chainHash }}</dd><dt>当前摘要</dt><dd class="hash">{{ proof.currentHash }}</dd></dl>
        <el-button type="success" size="large" style="width:100%" :loading="verifying" @click="verify">重新计算并核验</el-button>
      </section>
      <section class="explain"><h3>核验说明</h3><p>系统读取业务数据和文件 SHA-256 指纹，重新计算当前摘要，并与区块链上的历史摘要比对。</p></section>
    </template>
  </MobileShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import MobileShell from '@/components/mini/MobileShell.vue'
import { traceabilityApi } from '@/api/modules/traceability'
import type { ChainProofRecord } from '@/types/traceability'

const route=useRoute(),code=String(route.query.code||'RC20260718000001'),proof=ref<ChainProofRecord|null>(null),loading=ref(false),verifying=ref(false)
async function load(){loading.value=true;try{proof.value=(await traceabilityApi.getChainProof('TRACE_CODE',code)).data}finally{loading.value=false}}
async function verify(){verifying.value=true;try{proof.value=(await traceabilityApi.verifyChainProof('TRACE_CODE',code)).data;proof.value?.verified?ElMessage.success('链上核验通过'):ElMessage.error('链上核验异常')}finally{verifying.value=false}}
onMounted(load)
</script>

<style scoped>
.result,.chain-card,.explain{padding:18px;margin-bottom:14px;border-radius:16px;background:#fff;box-shadow:0 4px 18px #1a56320d}.result{text-align:center}.result>.el-icon{font-size:58px}.result h2{margin:5px 0}.result p{margin:0;font-size:13px}.result.ok{color:#197442;background:#ebf8ef}.result.bad{color:#b02f2f;background:#fff0f0}.chain-title{display:flex;align-items:center;gap:12px;padding-bottom:14px;border-bottom:1px solid #edf0ee}.block{display:grid;place-items:center;width:52px;height:52px;border-radius:12px;color:#fff;background:#183f2a;font-size:10px}.chain-title p{margin:4px 0 0;color:#829087;font-size:11px}.chain-card dl{display:grid;grid-template-columns:82px 1fr;gap:12px;margin:18px 0;font-size:12px}.chain-card dt{color:#87948c}.chain-card dd{margin:0;text-align:right}.hash{overflow-wrap:anywhere;font-family:Consolas,monospace;font-size:11px}.explain h3{margin:0 0 8px}.explain p{margin:0;color:#68766d;font-size:12px;line-height:1.7}
</style>
