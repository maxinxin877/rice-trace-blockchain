<template>
  <div class="page-container">
    <div class="page-header" v-if="title || $slots.actions">
      <h2 class="page-title">{{ title }}</h2>
      <div class="page-actions">
        <slot name="actions" />
      </div>
    </div>
    <div class="page-body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
}>()
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 6px;
  padding: 20px 24px;
  min-height: calc(100vh - 130px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e5e6eb;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
  margin: 0;
}

.page-actions {
  display: flex;
  gap: 10px;
}
</style>
