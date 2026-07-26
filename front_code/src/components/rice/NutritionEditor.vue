<template>
  <div class="nutrition-editor">
    <div
      v-for="(item, key) in localValue"
      :key="key"
      class="nutrition-row"
    >
      <el-input
        :model-value="key"
        disabled
        style="width: 130px"
        placeholder="项目名"
        size="small"
      />
      <el-input
        v-model="localValue[key]"
        style="width: 180px"
        placeholder="如: 1448kJ/100g"
        size="small"
      />
    </div>
    <el-button type="primary" link size="small" @click="showAdd = true">
      + 添加营养项目
    </el-button>
    <div v-if="showAdd" class="add-row">
      <el-input v-model="newKey" placeholder="项目名" size="small" style="width: 130px" />
      <el-input v-model="newValue" placeholder="含量" size="small" style="width: 180px" />
      <el-button type="primary" size="small" @click="confirmAdd">添加</el-button>
      <el-button size="small" @click="showAdd = false">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { NutritionFacts } from '@/types/productBatch'

const props = defineProps<{
  modelValue?: NutritionFacts
}>()

const emit = defineEmits<{
  'update:modelValue': [value: NutritionFacts]
}>()

const localValue = reactive<Record<string, string>>({ ...props.modelValue } || {
  energy: '',
  protein: '',
  fat: '',
  carbohydrate: '',
  sodium: '',
})

const showAdd = ref(false)
const newKey = ref('')
const newValue = ref('')

watch(localValue, (val) => {
  emit('update:modelValue', { ...val })
}, { deep: true })

function confirmAdd() {
  if (newKey.value && newValue.value) {
    localValue[newKey.value] = newValue.value
    newKey.value = ''
    newValue.value = ''
    showAdd.value = false
  }
}
</script>

<style scoped>
.nutrition-editor {
  padding: 8px 0;
}

.nutrition-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.add-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}
</style>
