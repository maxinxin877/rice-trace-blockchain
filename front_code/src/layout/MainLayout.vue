<template>
  <el-container class="app-layout">
    <el-aside width="240px">
      <h1 class="app-title">水稻溯源</h1>
      <el-menu router>
        <el-menu-item index="/rice/dashboard">看板</el-menu-item>
        <el-menu-item index="/rice/fields">地块档案</el-menu-item>
        <el-menu-item index="/rice/planting-batches">种植批次</el-menu-item>
        <el-menu-item index="/rice/storage-receipts">收储入库</el-menu-item>
        <el-menu-item index="/rice/milling-batches">碾米加工</el-menu-item>
        <el-menu-item index="/rice/product-batches">成品批次</el-menu-item>
        <el-menu-item index="/rice/trace-codes">防伪码</el-menu-item>
        <el-menu-item index="/rice/regulation/risk-warnings">风险预警</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.app-title {
  margin: 16px;
  font-size: 18px;
}
</style>
