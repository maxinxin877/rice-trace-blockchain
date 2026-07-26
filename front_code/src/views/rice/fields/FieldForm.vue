<template>
  <PageContainer :title="isEdit ? '编辑地块' : '新增地块'">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
      style="max-width: 800px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="地块编号" prop="fieldCode">
            <el-input v-model="form.fieldCode" placeholder="如: HLJ-WC-001" :disabled="isEdit" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="地块名称" prop="fieldName">
            <el-input v-model="form.fieldName" placeholder="如: 五常一号稻田" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="种植户ID" prop="farmerId">
            <el-input v-model="form.farmerId" placeholder="种植户ID" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="种植户名称" prop="farmerName">
            <el-input v-model="form.farmerName" placeholder="种植户姓名" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">所在地区</el-divider>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="省" prop="province">
            <el-input v-model="form.province" placeholder="如: 黑龙江省" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="市" prop="city">
            <el-input v-model="form.city" placeholder="如: 哈尔滨市" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="区/县" prop="district">
            <el-input v-model="form.district" placeholder="如: 五常市" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="详细地址">
        <el-input v-model="form.address" placeholder="如: 五常镇民乐村三组" />
      </el-form-item>

      <el-divider content-position="left">地块信息</el-divider>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="地块面积(亩)" prop="areaMu">
            <el-input-number v-model="form.areaMu" :min="0.01" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="土壤类型">
            <el-input v-model="form.soilType" placeholder="如: 黑土、草甸土" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">GIS 中心坐标</el-divider>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="中心经度" prop="centerLng">
            <el-input-number
              v-model="form.centerLng"
              :min="-180"
              :max="180"
              :precision="6"
              :step="0.01"
              placeholder="如: 127.5678"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="中心纬度" prop="centerLat">
            <el-input-number
              v-model="form.centerLat"
              :min="-90"
              :max="90"
              :precision="6"
              :step="0.01"
              placeholder="如: 45.1234"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <div class="form-tip">输入地块中心点坐标（WGS84坐标系），经度范围 -180~180，纬度范围 -90~90</div>

      <el-divider content-position="left">照片</el-divider>

      <el-form-item label="基地照片">
        <FileUpload v-model="form.basePhotoFileIds" accept=".jpg,.jpeg,.png,.webp" :limit="9" tip="支持 jpg/png/webp，每张不超过10MB" />
      </el-form-item>

      <el-form-item v-if="isEdit" label="修改原因">
        <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="请输入修改原因（审计需要）" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import FileUpload from '@/components/common/FileUpload.vue'
import { fieldApi } from '@/api/modules/field'
import type { GisPoint } from '@/types/field'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()

const isEdit = computed(() => !!route.params.id)

const form = reactive<{
  fieldId?: string
  fieldCode: string
  fieldName: string
  farmerId: string
  farmerName: string
  province: string
  city: string
  district: string
  address: string
  areaMu: number
  soilType: string
  centerLng: number | undefined
  centerLat: number | undefined
  basePhotoFileIds: string[]
  reason: string
}>({
  fieldCode: '',
  fieldName: '',
  farmerId: '',
  farmerName: '',
  province: '',
  city: '',
  district: '',
  address: '',
  areaMu: 0,
  soilType: '',
  centerLng: undefined,
  centerLat: undefined,
  basePhotoFileIds: [],
  reason: '',
})

const rules: FormRules = {
  fieldCode: [{ required: true, message: '请输入地块编号', trigger: 'blur' }],
  fieldName: [{ required: true, message: '请输入地块名称', trigger: 'blur' }],
  farmerId: [{ required: true, message: '请输入种植户ID', trigger: 'blur' }],
  farmerName: [{ required: true, message: '请输入种植户名称', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省', trigger: 'blur' }],
  city: [{ required: true, message: '请输入市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  areaMu: [{ required: true, message: '请输入地块面积', trigger: 'blur' }],
  centerLng: [
    { required: true, message: '请输入中心经度', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== undefined && (value < -180 || value > 180)) {
          return callback(new Error('经度范围应在 -180 到 180 之间'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  centerLat: [
    { required: true, message: '请输入中心纬度', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== undefined && (value < -90 || value > 90)) {
          return callback(new Error('纬度范围应在 -90 到 90 之间'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const submitting = ref(false)

onMounted(async () => {
  if (isEdit.value) {
    const id = route.params.id as string
    const res = await fieldApi.getById(id)
    if (res.code === 200 && res.data) {
      const d = res.data
      // 尝试从 gisBoundary 取第一个点作为中心坐标
      const center = d.gisBoundary && d.gisBoundary.length > 0 ? d.gisBoundary[0] : null
      Object.assign(form, {
        fieldId: d.fieldId,
        fieldCode: d.fieldCode,
        fieldName: d.fieldName,
        farmerId: d.farmerId,
        farmerName: d.farmerName,
        province: d.province,
        city: d.city,
        district: d.district,
        address: d.address,
        areaMu: d.areaMu,
        soilType: d.soilType || '',
        centerLng: center?.lng,
        centerLat: center?.lat,
        basePhotoFileIds: d.basePhotoFileIds || [],
        reason: '',
      })
    }
  }
})

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 用中心坐标构建 gisBoundary
    const centerPoint: GisPoint = {
      lng: form.centerLng!,
      lat: form.centerLat!,
    }

    if (isEdit.value) {
      await fieldApi.update({
        fieldId: form.fieldId!,
        fieldName: form.fieldName,
        farmerId: form.farmerId,
        farmerName: form.farmerName,
        province: form.province,
        city: form.city,
        district: form.district,
        address: form.address,
        areaMu: form.areaMu,
        soilType: form.soilType,
        gisBoundary: [centerPoint],
        basePhotoFileIds: form.basePhotoFileIds,
        reason: form.reason,
      })
      ElMessage.success('地块更新成功')
    } else {
      await fieldApi.create({
        fieldCode: form.fieldCode,
        fieldName: form.fieldName,
        farmerId: form.farmerId,
        farmerName: form.farmerName,
        province: form.province,
        city: form.city,
        district: form.district,
        address: form.address,
        areaMu: form.areaMu,
        gisBoundary: [centerPoint],
        soilType: form.soilType,
        basePhotoFileIds: form.basePhotoFileIds,
      })
      ElMessage.success('地块创建成功')
    }
    router.back()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: -8px;
  margin-bottom: 16px;
  padding-left: 110px;
}
</style>
