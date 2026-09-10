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

      <el-form-item label="详细地址" prop="address">
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

      <el-divider content-position="left">GIS 边界坐标</el-divider>

      <div v-for="(point, index) in form.gisBoundary" :key="index" class="boundary-row">
        <el-row :gutter="12">
          <el-col :span="10">
            <el-form-item :label="`点${index + 1} 经度`" label-width="80px">
              <el-input-number v-model="point.lng" :min="-180" :max="180" :precision="6" :step="0.01" placeholder="经度" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="纬度" label-width="50px">
              <el-input-number v-model="point.lat" :min="-90" :max="90" :precision="6" :step="0.01" placeholder="纬度" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-button v-if="form.gisBoundary.length > 3" type="danger" plain @click="removeBoundaryPoint(index)">
              删除
            </el-button>
          </el-col>
        </el-row>
      </div>
      <el-button type="primary" plain @click="addBoundaryPoint">+ 添加边界点</el-button>
      <div class="form-tip">至少 3 个边界点，围成地块边界（WGS84坐标系，经度范围 -180~180，纬度范围 -90~90）</div>

      <el-divider content-position="left">照片</el-divider>

      <el-form-item label="基地照片">
        <FileUpload v-model="form.basePhotoFileIds" accept=".jpg,.jpeg,.png,.webp" :limit="9" tip="支持 jpg/png/webp，每张不超过10MB" />
      </el-form-item>

      <el-form-item v-if="isEdit" label="修改原因" prop="reason">
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
  gisBoundary: { lng: number | undefined; lat: number | undefined }[]
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
  gisBoundary: [
    { lng: undefined, lat: undefined },
    { lng: undefined, lat: undefined },
    { lng: undefined, lat: undefined },
  ],
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
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  areaMu: [{ required: true, message: '请输入地块面积', trigger: 'blur' }],
  // 编辑态才渲染该字段，创建态不会参与校验
  reason: [{ required: true, message: '请输入修改原因', trigger: 'blur' }],
}

const submitting = ref(false)

onMounted(async () => {
  if (isEdit.value) {
    const id = route.params.id as string
    const res = await fieldApi.getById(id)
    if (res.code === 200 && res.data) {
      const d = res.data
      const boundary = d.gisBoundary && d.gisBoundary.length > 0
        ? d.gisBoundary
        : [{ lng: undefined, lat: undefined }, { lng: undefined, lat: undefined }, { lng: undefined, lat: undefined }]
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
        gisBoundary: boundary.map((p) => ({ lng: p.lng, lat: p.lat })),
        basePhotoFileIds: d.basePhotoFileIds || [],
        reason: '',
      })
    }
  }
})

function addBoundaryPoint() {
  form.gisBoundary.push({ lng: undefined, lat: undefined })
}

function removeBoundaryPoint(index: number) {
  if (form.gisBoundary.length <= 3) return
  form.gisBoundary.splice(index, 1)
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 过滤出有效的边界点，并校验至少 3 个
  const points = form.gisBoundary.filter((p) => p.lng !== undefined && p.lat !== undefined)
  if (points.length < 3) {
    ElMessage.warning('GIS 边界至少需要 3 个有效坐标点')
    return
  }
  const gisBoundary: GisPoint[] = points.map((p) => ({ lng: p.lng!, lat: p.lat! }))

  submitting.value = true
  try {
    if (isEdit.value) {
      await fieldApi.update({
        fieldId: form.fieldId!,
        fieldCode: form.fieldCode,
        fieldName: form.fieldName,
        farmerId: form.farmerId,
        farmerName: form.farmerName,
        province: form.province,
        city: form.city,
        district: form.district,
        address: form.address,
        areaMu: form.areaMu,
        soilType: form.soilType,
        gisBoundary,
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
        gisBoundary,
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
  margin-top: 8px;
  margin-bottom: 16px;
}

.boundary-row {
  margin-bottom: 4px;
}
</style>
