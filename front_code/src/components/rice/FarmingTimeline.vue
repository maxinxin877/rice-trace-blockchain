<template>
  <div class="farming-timeline">
    <el-timeline v-if="logs.length > 0">
      <el-timeline-item
        v-for="log in logs"
        :key="log.logId"
        :timestamp="formatDateTime(log.operationTime)"
        placement="top"
        :color="getColor(log.operationType)"
      >
        <el-card shadow="hover">
          <div class="timeline-header">
            <StatusTag type="farming" :value="log.operationType" />
            <span class="operator">操作人: {{ log.operatorName }}</span>
          </div>
          <div class="timeline-body">
            <p v-if="log.description">{{ log.description }}</p>
            <div class="material-info" v-if="log.materialName">
              <el-tag size="small" type="info">投入品: {{ log.materialName }}</el-tag>
              <el-tag v-if="log.materialDosage" size="small" type="info">
                用量: {{ log.materialDosage }} {{ log.materialUnit || '' }}
              </el-tag>
              <el-tag v-if="log.safeIntervalDays != null" size="small" type="warning">
                安全间隔期: {{ log.safeIntervalDays }} 天
              </el-tag>
            </div>
          </div>
          <div class="timeline-footer">
            <StatusTag type="chain" :value="log.chainStatus" />
            <span v-if="log.txId" class="tx-id">TX: {{ log.txId.slice(0, 16) }}...</span>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无农事记录" :image-size="80" />
  </div>
</template>

<script setup lang="ts">
import StatusTag from '@/components/common/StatusTag.vue'
import { formatDateTime } from '@/utils/format'
import { FarmingOperationType } from '@/types/enums'
import type { RiceFarmingLog } from '@/types/farmingLog'

defineProps<{
  logs: RiceFarmingLog[]
}>()

const colorMap: Record<string, string> = {
  [FarmingOperationType.SOWING]: '#409EFF',
  [FarmingOperationType.FERTILIZING]: '#67C23A',
  [FarmingOperationType.PESTICIDE]: '#E6A23C',
  [FarmingOperationType.IRRIGATION]: '#00BCD4',
  [FarmingOperationType.WEEDING]: '#909399',
  [FarmingOperationType.HARVESTING]: '#FF9800',
}

function getColor(type: string): string {
  return colorMap[type] || '#909399'
}
</script>

<style scoped>
.farming-timeline {
  padding: 10px 0;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.operator {
  font-size: 13px;
  color: #909399;
}

.timeline-body p {
  margin: 6px 0;
  font-size: 14px;
  color: #606266;
}

.material-info {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.timeline-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}

.tx-id {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
