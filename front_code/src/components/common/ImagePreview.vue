<template>
  <div class="image-preview">
    <div class="image-grid" v-if="urls.length > 0">
      <div
        v-for="(url, index) in urls"
        :key="index"
        class="image-item"
        @click="previewIndex = index"
      >
        <el-image
          :src="url"
          fit="cover"
          style="width: 120px; height: 120px; border-radius: 4px; cursor: pointer"
        >
          <template #error>
            <div class="image-error">
              <el-icon><PictureFilled /></el-icon>
            </div>
          </template>
        </el-image>
      </div>
    </div>
    <el-empty v-else description="暂无图片" :image-size="60" />

    <!-- 预览 -->
    <el-image-viewer
      v-if="previewIndex >= 0"
      :url-list="urls"
      :initial-index="previewIndex"
      @close="previewIndex = -1"
      :hide-on-click-modal="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  urls: string[]
}>()

const previewIndex = ref(-1)
</script>

<style scoped>
.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.image-error {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 32px;
}
</style>
