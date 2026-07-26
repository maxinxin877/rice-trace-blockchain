<template>
  <PageContainer title="地块详情">
    <template #actions>
      <el-button @click="$router.back()">返回</el-button>
      <el-button type="primary" @click="$router.push(`/rice/fields/${fieldId}/edit`)">编辑</el-button>
    </template>

    <div v-loading="loading" class="detail-content">
      <template v-if="field">
        <!-- 基本信息 -->
        <el-descriptions title="基本信息" :column="3" border style="margin-bottom: 20px">
          <el-descriptions-item label="地块编号">{{ field.fieldCode }}</el-descriptions-item>
          <el-descriptions-item label="地块名称">{{ field.fieldName }}</el-descriptions-item>
          <el-descriptions-item label="种植户">{{ field.farmerName }}</el-descriptions-item>
          <el-descriptions-item label="所在地区">{{ field.province }} {{ field.city }} {{ field.district }}</el-descriptions-item>
          <el-descriptions-item label="详细地址">{{ field.address }}</el-descriptions-item>
          <el-descriptions-item label="地块面积">{{ formatArea(field.areaMu) }}</el-descriptions-item>
          <el-descriptions-item label="土壤类型">{{ field.soilType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="坐标哈希">{{ field.coordinateHash || '-' }}</el-descriptions-item>
          <el-descriptions-item label="链上状态">
            <StatusTag type="chain" :value="field.chainStatus" />
            <span v-if="field.txId" class="tx-id"> TX: {{ field.txId.slice(0, 20) }}...</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- GIS 中心坐标 -->
        <div class="section">
          <h3>GIS 中心坐标</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="中心经度">
              {{ field.gisBoundary?.[0]?.lng ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="中心纬度">
              {{ field.gisBoundary?.[0]?.lat ?? '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 基地照片 -->
        <div class="section" v-if="field.basePhotoFileIds && field.basePhotoFileIds.length > 0">
          <h3>基地照片 ({{ field.basePhotoFileIds.length }} 张)</h3>
          <div class="photo-grid">
            <div v-for="(fid, idx) in field.basePhotoFileIds" :key="idx" class="photo-item">
              <el-image
                :src="getImageUrl(fid)"
                fit="cover"
                style="width: 160px; height: 120px; border-radius: 4px"
                :preview-src-list="getPreviewUrls()"
                :initial-index="idx"
              >
                <template #error>
                  <div class="photo-placeholder">
                    <el-icon :size="32"><PictureFilled /></el-icon>
                    <span>照片 {{ idx + 1 }}</span>
                  </div>
                </template>
              </el-image>
              <span class="photo-id">{{ fid.slice(0, 16) }}...</span>
            </div>
          </div>
        </div>

        <!-- 种植批次 -->
        <div class="section">
          <div class="section-header">
            <h3>关联种植批次</h3>
            <el-button type="primary" size="small" @click="showBatchForm = true">新增种植批次</el-button>
          </div>
          <el-table :data="batches" border stripe style="width: 100%">
            <el-table-column prop="plantingBatchId" label="批次ID" width="180" />
            <el-table-column prop="riceVariety" label="水稻品种" width="140" />
            <el-table-column prop="sowingDate" label="播种日期" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <StatusTag type="batch" :value="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="链上状态" width="90">
              <template #default="{ row }">
                <StatusTag type="chain" :value="row.chainStatus" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="$router.push(`/rice/planting-batches/detail/${row.plantingBatchId}`)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </div>

    <!-- 新增种植批次弹窗 -->
    <el-dialog v-model="showBatchForm" title="新增种植批次" width="600px" @close="batchFormRef?.resetFields()">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="120px">
        <el-form-item label="水稻品种" prop="riceVariety">
          <el-input v-model="batchForm.riceVariety" placeholder="如: 五优稻4号" />
        </el-form-item>
        <el-form-item label="种子来源" prop="seedSource">
          <el-input v-model="batchForm.seedSource" placeholder="如: 五常市种子公司" />
        </el-form-item>
        <el-form-item label="种子批号">
          <el-input v-model="batchForm.seedBatchNo" placeholder="种子批号" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="播种日期" prop="sowingDate">
              <el-date-picker v-model="batchForm.sowingDate" type="date" value-format="YYYY-MM-DD" placeholder="播种日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计收割日期">
              <el-date-picker v-model="batchForm.expectedHarvestDate" type="date" value-format="YYYY-MM-DD" placeholder="预计收割日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="有机认证">
              <el-switch v-model="batchForm.organicCertified" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绿色认证">
              <el-switch v-model="batchForm.greenCertified" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showBatchForm = false">取消</el-button>
        <el-button type="primary" @click="createBatch" :loading="creating">确认创建</el-button>
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
import { fieldApi } from '@/api/modules/field'
import { plantingBatchApi } from '@/api/modules/plantingBatch'
import { formatArea } from '@/utils/format'
import { mockImageCache } from '@/api/mock/imageCache'
import type { RiceField } from '@/types/field'
import type { RicePlantingBatch } from '@/types/plantingBatch'

const route = useRoute()
const fieldId = route.params.id as string

const loading = ref(false)
const field = ref<RiceField | null>(null)
const batches = ref<RicePlantingBatch[]>([])

// 新增批次
const showBatchForm = ref(false)
const creating = ref(false)
const batchFormRef = ref<FormInstance>()
const batchForm = reactive({
  riceVariety: '',
  seedSource: '',
  seedBatchNo: '',
  sowingDate: '',
  expectedHarvestDate: '',
  organicCertified: false,
  greenCertified: false,
})

const batchRules: FormRules = {
  riceVariety: [{ required: true, message: '请输入水稻品种', trigger: 'blur' }],
  seedSource: [{ required: true, message: '请输入种子来源', trigger: 'blur' }],
  sowingDate: [{ required: true, message: '请选择播种日期', trigger: 'change' }],
}

onMounted(async () => {
  loading.value = true
  try {
    const [fieldRes, batchRes] = await Promise.all([
      fieldApi.getById(fieldId),
      plantingBatchApi.getList({ fieldId, pageSize: 100 }),
    ])
    if (fieldRes.code === 200) field.value = fieldRes.data
    if (batchRes.code === 200) batches.value = batchRes.data.records
  } finally {
    loading.value = false
  }
})

async function createBatch() {
  if (!batchFormRef.value) return
  const valid = await batchFormRef.value.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    await plantingBatchApi.create({
      fieldId,
      riceVariety: batchForm.riceVariety,
      seedSource: batchForm.seedSource,
      seedBatchNo: batchForm.seedBatchNo,
      sowingDate: batchForm.sowingDate,
      expectedHarvestDate: batchForm.expectedHarvestDate,
      organicCertified: batchForm.organicCertified,
      greenCertified: batchForm.greenCertified,
    })
    ElMessage.success('种植批次创建成功')
    showBatchForm.value = false
    // 刷新列表
    const res = await plantingBatchApi.getList({ fieldId, pageSize: 100 })
    if (res.code === 200) batches.value = res.data.records
  } catch {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

function getImageUrl(fid: string): string {
  return mockImageCache.get(fid) || ''
}

function getPreviewUrls(): string[] {
  return (field.value?.basePhotoFileIds || [])
    .map((fid) => mockImageCache.get(fid))
    .filter(Boolean) as string[]
}
</script>

<style scoped>
.detail-content {
  max-width: 1000px;
}

.section {
  margin-bottom: 24px;
}

.section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-header h3 {
  margin-bottom: 0;
}

.tx-id {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: 6px;
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.photo-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.photo-placeholder {
  width: 160px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  border-radius: 4px;
  font-size: 12px;
  gap: 4px;
}

.photo-id {
  font-size: 10px;
  color: #c0c4cc;
}
</style>
