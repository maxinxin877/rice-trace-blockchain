<template>
  <div class="quality-form">
    <el-form-item label="检测机构" prop="testAgency">
      <el-input v-model="form.testAgency" placeholder="请输入检测机构名称" />
    </el-form-item>
    <el-form-item label="检测时间" prop="testTime">
      <el-date-picker
        v-model="form.testTime"
        type="datetime"
        value-format="YYYY-MM-DD HH:mm:ss"
        placeholder="选择检测时间"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="综合结论" prop="overallResult">
      <el-radio-group v-model="form.overallResult">
        <el-radio value="PASS">合格</el-radio>
        <el-radio value="FAIL">不合格</el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- 检测项目列表 -->
    <div class="test-items">
      <div class="test-items-header">
        <span class="label">检测项目</span>
        <el-button type="primary" size="small" @click="addItem">+ 添加项目</el-button>
      </div>
      <div
        v-for="(item, index) in form.items"
        :key="index"
        class="test-item-row"
      >
        <el-input v-model="item.itemName" placeholder="项目名称" style="width: 150px" />
        <el-input-number v-model="item.itemValue" :precision="4" :step="0.01" style="width: 150px" />
        <el-input v-model="item.unit" placeholder="单位" style="width: 80px" />
        <el-input v-model="item.standardValue" placeholder="标准值" style="width: 120px" />
        <el-select v-model="item.result" style="width: 100px">
          <el-option label="合格" value="PASS" />
          <el-option label="不合格" value="FAIL" />
        </el-select>
        <el-button type="danger" size="small" circle @click="removeItem(index)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>

    <el-form-item label="备注">
      <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="检测备注" />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

export interface QualityFormData {
  testAgency: string
  testTime: string
  overallResult: string
  remark: string
  items: {
    itemName: string
    itemValue: number
    unit: string
    standardValue: string
    result: string
  }[]
}

const props = defineProps<{
  modelValue: QualityFormData
}>()

const emit = defineEmits<{
  'update:modelValue': [value: QualityFormData]
}>()

const form = reactive<QualityFormData>({
  testAgency: props.modelValue?.testAgency || '',
  testTime: props.modelValue?.testTime || '',
  overallResult: props.modelValue?.overallResult || 'PASS',
  remark: props.modelValue?.remark || '',
  items: props.modelValue?.items?.length ? [...props.modelValue.items] : [],
})

watch(form, (val) => {
  emit('update:modelValue', { ...val })
}, { deep: true })

function addItem() {
  form.items.push({
    itemName: '',
    itemValue: 0,
    unit: '',
    standardValue: '',
    result: 'PASS',
  })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}
</script>

<style scoped>
.test-items {
  margin: 12px 0;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}

.test-items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.test-items-header .label {
  font-size: 14px;
  color: #606266;
}

.test-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
