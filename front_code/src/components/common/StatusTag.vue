<template>
  <el-tag>{{ value }}</el-tag>
</template>

<script setup>
defineProps({
  value: { type: String, default: '-' }
})
</script>
