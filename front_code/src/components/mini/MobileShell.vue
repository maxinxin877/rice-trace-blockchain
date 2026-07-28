<template>
  <div class="mobile-page">
    <header class="mobile-header">
      <button v-if="back" class="back" @click="router.back()">‹</button>
      <div><h1>{{ title }}</h1><p v-if="subtitle">{{ subtitle }}</p></div>
      <span class="chain-badge">区块链存证</span>
    </header>
    <main><slot /></main>
    <footer>水稻产品质量安全溯源平台 · 数据真实可信</footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
defineProps<{ title: string; subtitle?: string; back?: boolean }>()
const router = useRouter()
</script>

<style scoped>
.mobile-page{min-height:100vh;background:#f3f7f2;color:#1f2d24}.mobile-header{position:relative;display:flex;align-items:center;justify-content:space-between;padding:22px 18px 26px;color:#fff;background:linear-gradient(145deg,#174c2d,#2f7a4d);border-radius:0 0 24px 24px;box-shadow:0 8px 24px #174c2d33}.mobile-header h1{margin:0;font-size:20px}.mobile-header p{margin:5px 0 0;color:#cce5d5;font-size:12px}.chain-badge{padding:6px 9px;border:1px solid #ffffff55;border-radius:14px;font-size:11px;background:#ffffff14}.back{width:32px;height:32px;margin-right:10px;border:0;border-radius:50%;color:#fff;background:#ffffff1f;font-size:25px}.mobile-header>div{flex:1}main{width:min(680px,100%);margin:0 auto;padding:16px}footer{text-align:center;padding:18px;color:#91a096;font-size:11px}
</style>
