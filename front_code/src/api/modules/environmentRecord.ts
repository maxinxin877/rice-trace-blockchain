/**
 * 环境数据 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceEnvironmentRecord, RiceEnvironmentRecordQuery } from '@/types/environmentRecord'
import { mockEnvironmentRecords } from '../mock/data/environmentRecords'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

/** 环境数据创建 DTO（plantingBatchId 由路径传入，不在 body 中） */
export interface RiceEnvironmentRecordCreateDTO {
  sourceType: string
  recordTime: string
  airTemperature?: number
  airHumidity?: number
  soilMoisture?: number
  rainfall?: number
  windSpeed?: number
  imageFileIds?: string[]
  remark?: string
}

async function mockGetList(query: RiceEnvironmentRecordQuery): Promise<PageResponse<RiceEnvironmentRecord>> {
  await mockDelay()
  let list = [...mockEnvironmentRecords]

  if (query.plantingBatchId) list = list.filter((r) => r.plantingBatchId === query.plantingBatchId)
  if (query.sourceType) list = list.filter((r) => r.sourceType === query.sourceType)

  list.sort((a, b) => new Date(b.recordTime).getTime() - new Date(a.recordTime).getTime())

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockCreate(plantingBatchId: string, dto: RiceEnvironmentRecordCreateDTO): Promise<ApiResponse<RiceEnvironmentRecord>> {
  await mockDelay()
  const newRecord: RiceEnvironmentRecord = {
    ...dto,
    sourceType: dto.sourceType as RiceEnvironmentRecord['sourceType'],
    environmentRecordId: mockId('ENV'),
    tenantId: 'TENANT001',
    plantingBatchId,
    createdBy: 'ADMIN001',
    createdAt: new Date().toISOString(),
  }
  mockEnvironmentRecords.unshift(newRecord)
  return { code: 200, message: '创建成功', data: newRecord }
}

async function realGetList(query: RiceEnvironmentRecordQuery): Promise<PageResponse<RiceEnvironmentRecord>> {
  const { plantingBatchId, sourceType, recordTimeStart, recordTimeEnd, ...rest } = query
  // 后端仅提供按种植批次嵌套的查询接口，无跨批次全量列表
  if (!plantingBatchId) {
    return {
      code: 200,
      message: 'success',
      data: { records: [], total: 0, page: rest.page || 1, pageSize: rest.pageSize || 10, totalPages: 0 },
    }
  }
  const params: Record<string, unknown> = { ...rest }
  if (sourceType) params.sourceType = sourceType
  if (recordTimeStart) params.startTime = recordTimeStart
  if (recordTimeEnd) params.endTime = recordTimeEnd
  const res = await get<PageResponse<RiceEnvironmentRecord>['data']>(`/rice/planting-batches/${plantingBatchId}/environment-records`, params)
  return res.data as unknown as PageResponse<RiceEnvironmentRecord>
}

async function realCreate(plantingBatchId: string, dto: RiceEnvironmentRecordCreateDTO): Promise<ApiResponse<RiceEnvironmentRecord>> {
  const res = await post<RiceEnvironmentRecord>(`/rice/planting-batches/${plantingBatchId}/environment-records`, dto)
  return res.data as ApiResponse<RiceEnvironmentRecord>
}

export const environmentRecordApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  create: USE_MOCK ? mockCreate : realCreate,
}
