/**
 * 环境数据 API 模块
 */
import type { PageResponse } from '@/types/api'
import type { RiceEnvironmentRecord, RiceEnvironmentRecordQuery } from '@/types/environmentRecord'
import { mockEnvironmentRecords } from '../mock/data/environmentRecords'
import { mockDelay, mockPaginate } from '../mock'
import { get } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

async function mockGetList(query: RiceEnvironmentRecordQuery): Promise<PageResponse<RiceEnvironmentRecord>> {
  await mockDelay()
  let list = [...mockEnvironmentRecords]

  if (query.plantingBatchId) list = list.filter((r) => r.plantingBatchId === query.plantingBatchId)
  if (query.sourceType) list = list.filter((r) => r.sourceType === query.sourceType)

  list.sort((a, b) => new Date(b.recordTime).getTime() - new Date(a.recordTime).getTime())

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function realGetList(query: RiceEnvironmentRecordQuery): Promise<PageResponse<RiceEnvironmentRecord>> {
  const res = await get<PageResponse<RiceEnvironmentRecord>['data']>('/rice/environment-records', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceEnvironmentRecord>
}

export const environmentRecordApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
}
