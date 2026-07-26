<template>
  <div class="process-params">
    <el-form-item label="脱壳工艺">
      <el-input v-model="localValue.husking" placeholder="如: 胶辊砻谷机，脱壳率≥85%" />
    </el-form-item>
    <el-form-item label="抛光工艺">
      <el-input v-model="localValue.polishing" placeholder="如: 三道抛光，白度≥38" />
    </el-form-item>
    <el-form-item label="色选工艺">
      <el-input v-model="localValue.colorSorting" placeholder="如: 色选机精选，剔除异色粒" />
    </el-form-item>
    <el-form-item label="包装工艺">
      <el-input v-model="localValue.packaging" placeholder="如: 真空包装，5kg/袋" />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { MillingProcessParams } from '@/types/millingBatch'

const props = defineProps<{
  modelValue: MillingProcessParams
}>()

const emit = defineEmits<{
  'update:modelValue': [value: MillingProcessParams]
}>()

const localValue = reactive<MillingProcessParams>({
  husking: props.modelValue?.husking || '',
  polishing: props.modelValue?.polishing || '',
  colorSorting: props.modelValue?.colorSorting || '',
  packaging: props.modelValue?.packaging || '',
})

watch(localValue, (val) => {
  emit('update:modelValue', { ...val })
}, { deep: true })
</script>
