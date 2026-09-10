import { ref, reactive } from 'vue'

/**
 * 通用表格逻辑：分页 + 查询 + loading
 */
export function useTable<T>(fetchFn: (params: Record<string, unknown>) => Promise<{ data: { records: T[]; total: number } }>) {
  const loading = ref(false)
  const data = ref<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(10)
  const query = reactive<Record<string, unknown>>({})

  async function loadData() {
    loading.value = true
    try {
      const res = await fetchFn({
        ...query,
        page: page.value,
        pageSize: pageSize.value,
      })
      data.value = res.data.records
      total.value = res.data.total
    } catch (e) {
      console.error('Failed to load data:', e)
      data.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  function onSearch(params: Record<string, unknown>) {
    Object.assign(query, params)
    page.value = 1
    loadData()
  }

  function onReset() {
    Object.keys(query).forEach((key) => {
      delete query[key]
    })
    page.value = 1
    loadData()
  }

  function onPageChange(p: number, ps: number) {
    page.value = p
    pageSize.value = ps
    loadData()
  }

  return {
    loading,
    data,
    total,
    page,
    pageSize,
    query,
    loadData,
    onSearch,
    onReset,
    onPageChange,
  }
}
