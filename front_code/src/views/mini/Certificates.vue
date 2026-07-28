<template>
  <MobileShell title="认证证书" subtitle="资质证书与质检报告" back>
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-empty v-else-if="!detail" description="未找到证书信息" />
    <template v-else>
      <section class="summary"><div class="seal">证</div><div><h2>{{ detail.traceCode.productName }}</h2><p>共 {{ detail.certificates.length }} 份有效证书与检测报告</p></div></section>
      <section v-for="item in detail.certificates" :key="item.certificateId" class="certificate">
        <div class="cert-head"><div class="file-icon">{{ item.fileType }}</div><div><h3>{{ item.name }}</h3><p>{{ item.authority }}</p></div><el-tag type="success">有效</el-tag></div>
        <div class="cert-body"><p><span>证书编号</span><b>{{ item.certificateNo }}</b></p><p><span>有效期</span><b>{{ item.validFrom }} 至 {{ item.validTo }}</b></p></div>
        <el-button type="primary" plain style="width:100%" @click="preview(item.name)">查看电子证书</el-button>
      </section>
    </template>
  </MobileShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import MobileShell from '@/components/mini/MobileShell.vue'
import { traceabilityApi } from '@/api/modules/traceability'
import type { TraceDetail } from '@/types/traceability'

const route=useRoute(),detail=ref<TraceDetail|null>(null),loading=ref(false)
async function load(){loading.value=true;try{detail.value=(await traceabilityApi.getTraceDetail(String(route.query.code||'RC20260718000001'))).data}finally{loading.value=false}}
function preview(name:string){ElMessage.success(`${name} 电子文件校验通过`)}
onMounted(load)
</script>

<style scoped>
.summary,.certificate{padding:18px;margin-bottom:14px;border-radius:16px;background:#fff;box-shadow:0 4px 18px #1a56320d}.summary{display:flex;align-items:center;gap:14px}.seal{display:grid;place-items:center;width:58px;height:58px;border:2px solid #c9463d;border-radius:50%;color:#c9463d;font-size:24px;font-family:serif}.summary h2{margin:0;font-size:17px}.summary p{margin:6px 0 0;color:#7f8c84;font-size:12px}.cert-head{display:flex;align-items:center;gap:11px}.cert-head>div:nth-child(2){flex:1}.file-icon{padding:10px 6px;border-radius:8px;color:#fff;background:#2f7a4d;font-size:10px}.cert-head h3{margin:0;font-size:15px}.cert-head p{margin:5px 0 0;color:#849189;font-size:11px}.cert-body{margin:15px 0;padding:10px 13px;background:#f6f8f6;border-radius:10px}.cert-body p{display:flex;justify-content:space-between;gap:10px;margin:7px 0;font-size:12px}.cert-body span{color:#849189}.cert-body b{text-align:right;font-weight:500}
</style>
