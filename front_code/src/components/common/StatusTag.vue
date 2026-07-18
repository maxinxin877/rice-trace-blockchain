<template>
  <el-tag
    :color="config.color"
    :style="{ color: '#fff', border: 'none' }"
    size="small"
  >
    {{ config.label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  CHAIN_STATUS_MAP,
  RICE_BATCH_STATUS_MAP,
  FARMING_OPERATION_TYPE_MAP,
  QUALITY_RESULT_MAP,
  RISK_LEVEL_MAP,
  ENVIRONMENT_SOURCE_TYPE_MAP,
  TRACE_CODE_STATUS_MAP,
} from '@/utils/constants'

type StatusType = 'chain' | 'batch' | 'farming' | 'quality' | 'risk' | 'environment' | 'traceCode'

const props = defineProps<{
  type: StatusType
  value: string
}>()

const statusMaps: Record<StatusType, Record<string, { label: string; color: string }>> = {
  chain: CHAIN_STATUS_MAP,
  batch: RICE_BATCH_STATUS_MAP,
  farming: FARMING_OPERATION_TYPE_MAP,
  quality: QUALITY_RESULT_MAP,
  risk: RISK_LEVEL_MAP,
  environment: ENVIRONMENT_SOURCE_TYPE_MAP,
  traceCode: TRACE_CODE_STATUS_MAP,
}

const config = computed(() => {
  const map = statusMaps[props.type]
  return map[props.value] || { label: props.value, color: '#909399' }
})
</script>
