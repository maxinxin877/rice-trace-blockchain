<template>
  <MobileShell title="真伪鉴别" subtitle="实时记录扫码并识别风险" back>
    <section class="card form-card">
      <el-input v-model="code" placeholder="防伪码" />
      <el-select v-model="region" style="width:100%" placeholder="当前扫码地区"><el-option label="北京市" value="北京市" /><el-option label="黑龙江省哈尔滨市" value="黑龙江省哈尔滨市" /><el-option label="广东省深圳市" value="广东省深圳市" /></el-select>
      <el-button type="success" size="large" :loading="loading" @click="verify">开始验真</el-button>
    </section>
    <section v-if="result" :class="['result-card',result.result.toLowerCase()]">
      <div class="result-icon"><el-icon><CircleCheckFilled v-if="result.verified" /><WarningFilled v-else /></el-icon></div>
      <h2>{{ result.title }}</h2><p>{{ result.message }}</p>
      <div class="scan-grid"><div><span>累计查询</span><b>{{ result.scanCount }} 次</b></div><div><span>首次查询</span><b>{{ result.firstScannedAt || '尚无记录' }}</b></div><div><span>本次查询</span><b>{{ result.currentScanAt }}</b></div></div>
      <el-alert v-for="tip in result.riskTips" :key="tip" :title="tip" type="warning" :closable="false" show-icon />
    </section>
    <section class="tips"><h3>安全提示</h3><p>首次查询且包装完整，可作为正品判断的重要依据。</p><p>若短时间多次查询或跨区域扫码，请核对购买渠道。</p></section>
  </MobileShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import MobileShell from '@/components/mini/MobileShell.vue'
import { traceabilityApi } from '@/api/modules/traceability'
import type { VerifyResult } from '@/types/traceability'

const route=useRoute(),code=ref(String(route.query.code||'RC20260718000001')),region=ref('北京市'),loading=ref(false),result=ref<VerifyResult>()
async function verify(){if(!code.value)return;loading.value=true;try{result.value=(await traceabilityApi.verifyTraceCode(code.value.trim(),region.value)).data}finally{loading.value=false}}
</script>

<style scoped>
.card,.result-card,.tips{padding:18px;margin-bottom:14px;border-radius:16px;background:#fff;box-shadow:0 4px 18px #1a56320d}.form-card{display:grid;gap:12px}.result-card{text-align:center;border:1px solid #dce9df}.result-card.genuine{background:#effaf2}.result-card.repeat{background:#fff9eb}.result-card.risk,.result-card.invalid{background:#fff1f0}.result-icon{font-size:58px;color:#2f8a52}.risk .result-icon,.invalid .result-icon{color:#c53030}.repeat .result-icon{color:#d97706}.result-card h2{margin:4px 0 8px}.result-card>p{color:#627068;font-size:13px}.scan-grid{display:grid;gap:1px;margin:18px 0;background:#e6ece8;border-radius:10px;overflow:hidden}.scan-grid div{padding:12px;background:#fff}.scan-grid span{display:block;color:#91a096;font-size:11px}.scan-grid b{display:block;margin-top:4px;font-size:13px}.result-card .el-alert{margin-top:8px;text-align:left}.tips h3{margin:0 0 10px}.tips p{margin:7px 0;color:#647168;font-size:12px}
</style>
