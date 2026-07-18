<template>
  <PageContainer title="种植批次详情">
    <template #actions>
      <el-button @click="$router.back()">返回</el-button>
    </template>

    <div v-loading="loading">
      <template v-if="batch">
        <el-descriptions title="批次信息" :column="3" border>
          <el-descriptions-item label="批次ID">{{ batch.plantingBatchId }}</el-descriptions-item>
          <el-descriptions-item label="水稻品种">{{ batch.riceVariety }}</el-descriptions-item>
          <el-descriptions-item label="种子来源">{{ batch.seedSource }}</el-descriptions-item>
          <el-descriptions-item label="种子批号">{{ batch.seedBatchNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="播种日期">{{ batch.sowingDate }}</el-descriptions-item>
          <el-descriptions-item label="预计收割">{{ batch.expectedHarvestDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际收割">{{ batch.actualHarvestDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="有机认证">{{ batch.organicCertified ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="绿色认证">{{ batch.greenCertified ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="批次状态">
            <StatusTag type="batch" :value="batch.status" />
          </el-descriptions-item>
          <el-descriptions-item label="链上状态">
            <StatusTag type="chain" :value="batch.chainStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="关联地块">
            <el-link type="primary" @click="$router.push(`/rice/fields/detail/${batch.fieldId}`)">
              {{ batch.fieldName }} ({{ batch.fieldCode }})
            </el-link>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 农事记录 -->
        <div class="section" style="margin-top: 24px">
          <div class="section-header">
            <h3>农事记录</h3>
            <el-button type="primary" size="small" @click="showFarmingForm = true">新增农事记录</el-button>
          </div>
          <FarmingTimeline :logs="farmingLogs" />
        </div>
      </template>
    </div>

    <!-- 新增农事记录弹窗 -->
    <el-dialog v-model="showFarmingForm" title="新增农事记录" width="650px">
      <el-form ref="farmingFormRef" :model="farmingForm" :rules="farmingRules" label-width="110px">
        <el-form-item label="农事类型" prop="operationType">
          <el-select v-model="farmingForm.operationType" style="width: 100%">
            <el-option
              v-for="(item, value) in FARMING_OPERATION_TYPE_MAP"
              :key="value"
              :label="item.label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间" prop="operationTime">
          <el-date-picker v-model="farmingForm.operationTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="操作人" prop="operatorName">
          <el-input v-model="farmingForm.operatorName" placeholder="操作人姓名" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="farmingForm.description" type="textarea" :rows="2" placeholder="操作描述" />
        </el-form-item>

        <!-- 用药信息（条件显示） -->
        <template v-if="farmingForm.operationType === 'PESTICIDE'">
          <el-divider content-position="left">投入品信息（用药必填）</el-divider>
          <el-form-item label="投入品名称" prop="materialName">
            <el-input v-model="farmingForm.materialName" placeholder="如: 吡虫啉" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用量" prop="materialDosage">
                <el-input-number v-model="farmingForm.materialDosage" :min="0.01" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="单位">
                <el-input v-model="farmingForm.materialUnit" placeholder="如: kg/亩" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="安全间隔期" prop="safeIntervalDays">
            <el-input-number v-model="farmingForm.safeIntervalDays" :min="0" style="width: 100%" />
            <span class="form-hint">单位：天</span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="showFarmingForm = false">取消</el-button>
        <el-button type="primary" @click="submitFarmingLog" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import FarmingTimeline from '@/components/rice/FarmingTimeline.vue'
import { plantingBatchApi } from '@/api/modules/plantingBatch'
import { farmingLogApi } from '@/api/modules/farmingLog'
import { FARMING_OPERATION_TYPE_MAP } from '@/utils/constants'
import type { RicePlantingBatch } from '@/types/plantingBatch'
import type { RiceFarmingLog } from '@/types/farmingLog'

const route = useRoute()
const batchId = route.params.id as string

const loading = ref(false)
const batch = ref<RicePlantingBatch | null>(null)
const farmingLogs = ref<RiceFarmingLog[]>([])

// 新增农事
const showFarmingForm = ref(false)
const submitting = ref(false)
const farmingFormRef = ref<FormInstance>()
const farmingForm = reactive({
  operationType: '',
  operationTime: '',
  operatorName: '',
  description: '',
  materialName: '',
  materialDosage: undefined as number | undefined,
  materialUnit: '',
  safeIntervalDays: undefined as number | undefined,
})

const farmingRules: FormRules = {
  operationType: [{ required: true, message: '请选择农事类型', trigger: 'change' }],
  operationTime: [{ required: true, message: '请选择操作时间', trigger: 'change' }],
  operatorName: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
  materialName: [
    {
      validator: (_rule, value, callback) => {
        if (farmingForm.operationType === 'PESTICIDE' && !value) {
          return callback(new Error('用药时投入品名称必填'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  materialDosage: [
    {
      validator: (_rule, value, callback) => {
        if (farmingForm.operationType === 'PESTICIDE' && (!value || value <= 0)) {
          return callback(new Error('用药时用量必须大于0'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  safeIntervalDays: [
    {
      validator: (_rule, value, callback) => {
        if (farmingForm.operationType === 'PESTICIDE' && (value === undefined || value < 0)) {
          return callback(new Error('用药时安全间隔期必须大于等于0'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

onMounted(async () => {
  loading.value = true
  try {
    const [batchRes, logRes] = await Promise.all([
      plantingBatchApi.getById(batchId),
      farmingLogApi.getList({ plantingBatchId: batchId, pageSize: 100 }),
    ])
    if (batchRes.code === 200) batch.value = batchRes.data
    if (logRes.code === 200) farmingLogs.value = logRes.data.records
  } finally {
    loading.value = false
  }
})

async function submitFarmingLog() {
  if (!farmingFormRef.value) return
  const valid = await farmingFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await farmingLogApi.create({
      plantingBatchId: batchId,
      operationType: farmingForm.operationType as never,
      operationTime: farmingForm.operationTime,
      operatorId: 'FARMER001',
      operatorName: farmingForm.operatorName,
      materialName: farmingForm.materialName || undefined,
      materialDosage: farmingForm.materialDosage,
      materialUnit: farmingForm.materialUnit || undefined,
      safeIntervalDays: farmingForm.safeIntervalDays,
      description: farmingForm.description || undefined,
    })
    ElMessage.success('农事记录添加成功')
    showFarmingForm.value = false
    // 刷新
    const res = await farmingLogApi.getList({ plantingBatchId: batchId, pageSize: 100 })
    if (res.code === 200) farmingLogs.value = res.data.records
  } catch {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.section {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h3 {
  margin-bottom: 0;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
