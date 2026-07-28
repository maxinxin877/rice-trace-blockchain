<template>
  <MobileShell title="扫码溯源" subtitle="一物一码 · 全程可信">
    <div class="query-card">
      <el-input v-model="code" placeholder="请输入包装上的防伪码" clearable @keyup.enter="loadDetail"><template #append><el-button :loading="loading" @click="loadDetail">查询</el-button></template></el-input>
      <button class="sample" @click="useSample">使用演示码 RC20260718000001</button>
    </div>
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="searched && !detail" description="未查询到该防伪码的溯源信息" />
    <template v-else-if="detail">
      <section class="product-card">
        <div class="rice-icon">稻</div>
        <div class="product-main"><span class="auth">官方溯源</span><h2>{{ detail.traceCode.productName }}</h2><p>{{ detail.traceCode.brandName }} · {{ detail.traceCode.riceVariety }} · {{ detail.traceCode.packageSpec }}</p></div>
        <el-tag type="success" effect="dark">已激活</el-tag>
      </section>
      <section class="verify-strip"><el-icon><CircleCheckFilled /></el-icon><div><strong>产品身份有效</strong><p>防伪码已激活，关键生产数据已上链存证</p></div><span>扫码 {{ detail.traceCode.scanCount }} 次</span></section>
      <section class="card info-grid">
        <div><span>产地</span><b>{{ detail.origin }}</b></div><div><span>基地</span><b>{{ detail.fieldName }}</b></div><div><span>收获日期</span><b>{{ detail.harvestDate }}</b></div><div><span>生产日期</span><b>{{ detail.productionDate }}</b></div><div><span>质检结果</span><b class="green">{{ detail.qualityResult }}</b></div><div><span>销售区域</span><b>{{ detail.traceCode.expectedSaleRegion }}</b></div>
      </section>
      <section class="card"><h3>全链路溯源</h3><el-timeline><el-timeline-item v-for="node in detail.timeline" :key="node.businessId" :timestamp="node.time" color="#2f7a4d"><div class="node"><div><strong>{{ node.title }}</strong><StatusTag type="chain" :value="node.chainStatus" /></div><p>{{ node.summary }}</p><small>{{ node.organization }} · {{ node.location }}</small></div></el-timeline-item></el-timeline></section>
      <section class="card"><h3>营养成分</h3><div class="nutrition"><div v-for="(value,key) in detail.nutritionFacts" :key="key"><span>{{ key }}</span><b>{{ value }}</b></div></div></section>
      <div class="actions"><el-button type="success" @click="go('/pages/rice/trace/verify')">立即验真</el-button><el-button @click="go('/pages/rice/trace/certificates')">查看证书</el-button><el-button @click="go('/pages/rice/trace/chain-proof')">链上核验</el-button></div>
    </template>
  </MobileShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileShell from '@/components/mini/MobileShell.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { traceabilityApi } from '@/api/modules/traceability'
import type { TraceDetail } from '@/types/traceability'

const route=useRoute(),router=useRouter(),code=ref(String(route.query.code||'RC20260718000001')),detail=ref<TraceDetail|null>(null),loading=ref(false),searched=ref(false)
async function loadDetail(){if(!code.value)return;loading.value=true;try{detail.value=(await traceabilityApi.getTraceDetail(code.value.trim())).data;searched.value=true}finally{loading.value=false}}
function useSample(){code.value='RC20260718000001';loadDetail()}
function go(path:string){router.push({path,query:{code:code.value}})}
onMounted(loadDetail)
</script>

<style scoped>
.query-card,.card,.product-card{margin-bottom:14px;padding:16px;background:#fff;border-radius:14px;box-shadow:0 4px 18px #1a56320d}.sample{margin-top:9px;padding:0;border:0;color:#2f7a4d;background:none;font-size:12px}.product-card{display:flex;align-items:center;gap:12px}.rice-icon{display:grid;place-items:center;width:58px;height:58px;border-radius:16px;color:#fff;background:linear-gradient(145deg,#e6b64d,#b97d1c);font-size:25px;font-family:serif}.product-main{flex:1}.product-main h2{margin:5px 0;font-size:18px}.product-main p{margin:0;color:#738078;font-size:12px}.auth{color:#2f7a4d;font-size:11px}.verify-strip{display:flex;align-items:center;gap:10px;margin-bottom:14px;padding:14px;color:#176b3a;background:#e8f6ed;border-radius:14px}.verify-strip>.el-icon{font-size:28px}.verify-strip div{flex:1}.verify-strip p{margin:3px 0 0;font-size:11px}.verify-strip>span{font-size:11px}.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.info-grid span,.nutrition span{display:block;color:#91a096;font-size:11px}.info-grid b,.nutrition b{display:block;margin-top:4px;font-size:13px}.green{color:#23814a}.card h3{margin:0 0 16px;font-size:16px}.node>div{display:flex;justify-content:space-between}.node p{margin:7px 0;color:#536158;font-size:12px}.node small{color:#91a096}.nutrition{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}.actions{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.actions .el-button{margin:0;padding:8px 4px}@media(max-width:420px){.actions{grid-template-columns:1fr}.info-grid{grid-template-columns:1fr}}
</style>
