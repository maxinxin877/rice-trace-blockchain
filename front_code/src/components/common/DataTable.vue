<template>
  <div class="data-table">
    <el-table
      :data="data"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <!-- 多选列 -->
      <el-table-column
        v-if="showSelection"
        type="selection"
        width="50"
        align="center"
      />
      <!-- 动态列 -->
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :sortable="col.sortable"
        :align="col.align || 'center'"
      >
        <template v-if="col.slot" #default="scope">
          <slot :name="col.slot" :row="scope.row" :$index="scope.$index" />
        </template>
      </el-table-column>
      <!-- 操作列 -->
      <el-table-column
        v-if="$slots.actions"
        label="操作"
        :width="actionWidth"
        align="center"
        fixed="right"
      >
        <template #default="scope">
          <slot name="actions" :row="scope.row" :$index="scope.$index" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty
      v-if="!loading && data.length === 0"
      description="暂无数据"
      :image-size="120"
    />

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="showPagination && total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

export interface TableColumn {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  sortable?: boolean | string
  align?: 'left' | 'center' | 'right'
  slot?: string
}

const props = defineProps<{
  columns: TableColumn[]
  data: Record<string, unknown>[]
  loading?: boolean
  total?: number
  page?: number
  pageSize?: number
  showSelection?: boolean
  showPagination?: boolean
  actionWidth?: string | number
}>()

const emit = defineEmits<{
  'update:page': [page: number]
  'update:pageSize': [size: number]
  'page-change': [page: number, pageSize: number]
  'selection-change': [rows: Record<string, unknown>[]]
  'sort-change': [sort: { prop: string; order: string }]
}>()

const currentPage = ref(props.page || 1)
const currentPageSize = ref(props.pageSize || 10)

watch(() => props.page, (val) => {
  if (val !== undefined) currentPage.value = val
})

watch(() => props.pageSize, (val) => {
  if (val !== undefined) currentPageSize.value = val
})

function handlePageChange(page: number) {
  emit('update:page', page)
  emit('page-change', page, currentPageSize.value)
}

function handleSizeChange(size: number) {
  emit('update:pageSize', size)
  emit('page-change', currentPage.value, size)
}

function handleSelectionChange(rows: Record<string, unknown>[]) {
  emit('selection-change', rows)
}

function handleSortChange(sort: { prop: string; order: string }) {
  emit('sort-change', sort)
}
</script>

<style scoped>
.data-table {
  width: 100%;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}
</style>
