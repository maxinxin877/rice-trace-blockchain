<template>
  <div class="file-upload">
    <!-- Mock 模式：模拟上传 + 本地预览 -->
    <template v-if="isMock">
      <div class="mock-preview-list">
        <div v-for="(f, idx) in mockFiles" :key="f.fileId" class="mock-preview-item">
          <img v-if="f.previewUrl" :src="f.previewUrl" class="mock-thumb" />
          <div v-else class="mock-thumb mock-thumb-placeholder">
            <el-icon :size="28"><PictureFilled /></el-icon>
          </div>
          <span class="mock-name">{{ f.name }}</span>
          <el-icon class="mock-remove" @click="removeMockFile(idx)"><CircleClose /></el-icon>
        </div>
        <div
          v-if="mockFiles.length < (props.limit || 9)"
          class="mock-upload-btn"
          @click="mockUpload"
        >
          <el-icon :size="24"><Plus /></el-icon>
        </div>
      </div>
      <input
        ref="mockFileInput"
        type="file"
        :accept="accept"
        :multiple="multiple"
        style="display: none"
        @change="handleMockFileSelect"
      />
    </template>

    <!-- 真实模式：el-upload -->
    <template v-else>
      <el-upload
        :action="uploadUrl"
        :headers="uploadHeaders"
        :accept="accept"
        :multiple="multiple"
        :limit="limit"
        :on-exceed="handleExceed"
        :on-success="handleSuccess"
        :on-error="handleError"
        :before-upload="beforeUpload"
        :file-list="realFileList"
        list-type="picture-card"
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
    </template>
    <div class="upload-tip" v-if="tip">{{ tip }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadProps, UploadUserFile } from 'element-plus'
import { mockImageCache } from '@/api/mock/imageCache'

const props = defineProps<{
  modelValue?: string[]
  accept?: string
  multiple?: boolean
  limit?: number
  maxSizeMB?: number
  tip?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [ids: string[]]
}>()

const isMock = import.meta.env.VITE_USE_MOCK === 'true'

// ========== 真实模式 ==========
const uploadUrl = computed(() => {
  const base = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  return `${base}/files/upload`
})

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
}))

const realFileList = ref<UploadUserFile[]>([])
const accept = computed(() => props.accept || '.jpg,.jpeg,.png,.webp,.pdf')

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const maxSize = (props.maxSizeMB || 10) * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxSizeMB || 10}MB`)
    return false
  }
  return true
}

const handleSuccess: UploadProps['onSuccess'] = (response: any) => {
  if (response?.code === 200 && response?.data?.fileId) {
    const ids = [...(props.modelValue || []), response.data.fileId]
    emit('update:modelValue', ids)
  }
}

const handleError: UploadProps['onError'] = () => {
  ElMessage.error('文件上传失败，请重试')
}

const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(`最多只能上传 ${props.limit || 5} 个文件`)
}

// ========== Mock 模式 ==========
const mockFileInput = ref<HTMLInputElement>()
const mockFiles = reactive<{ fileId: string; name: string; previewUrl: string }[]>([])

function mockUpload() {
  mockFileInput.value?.click()
}

function handleMockFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files) return

  const maxFiles = props.limit || 9
  if (mockFiles.length + files.length > maxFiles) {
    ElMessage.warning(`最多只能上传 ${maxFiles} 个文件`)
    input.value = ''
    return
  }

  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    const maxSize = (props.maxSizeMB || 10) * 1024 * 1024
    if (file.size > maxSize) {
      ElMessage.warning(`文件 "${file.name}" 超过 ${props.maxSizeMB || 10}MB 限制，已跳过`)
      continue
    }
    const fileId = `MOCK_FILE_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    const previewUrl = URL.createObjectURL(file)
    mockFiles.push({ fileId, name: file.name, previewUrl })

    // 转为 base64 存入缓存，供详情页等跨页面展示
    const reader = new FileReader()
    reader.onload = () => {
      mockImageCache.set(fileId, reader.result as string)
    }
    reader.readAsDataURL(file)

    const ids = [...(props.modelValue || []), fileId]
    emit('update:modelValue', ids)
  }
  ElMessage.success(`已添加 ${files.length} 张图片`)
  input.value = ''
}

function removeMockFile(index: number) {
  const removed = mockFiles.splice(index, 1)[0]
  if (removed.previewUrl) URL.revokeObjectURL(removed.previewUrl)
  const ids = (props.modelValue || []).filter((id) => id !== removed.fileId)
  emit('update:modelValue', ids)
}

onUnmounted(() => {
  mockFiles.forEach((f) => {
    if (f.previewUrl) URL.revokeObjectURL(f.previewUrl)
  })
})
</script>

<style scoped>
.file-upload {
  width: 100%;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

/* Mock 预览列表 */
.mock-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.mock-preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}

.mock-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mock-thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}

.mock-name {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 10px;
  padding: 2px 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mock-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  color: #f56c6c;
  background: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
}

.mock-upload-btn {
  width: 100px;
  height: 100px;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #8c939d;
  transition: border-color 0.3s;
}

.mock-upload-btn:hover {
  border-color: #409EFF;
  color: #409EFF;
}
</style>
