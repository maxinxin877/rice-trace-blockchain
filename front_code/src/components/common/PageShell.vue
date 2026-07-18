<template>
  <section class="page-shell">
    <header class="page-shell__header">
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </header>
    <slot />
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.page-shell {
  padding: 20px;
}

.page-shell__header {
  margin-bottom: 16px;
}
</style>
