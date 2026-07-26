/**
 * 加工批次 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceMillingBatch, RiceMillingBatchCreateDTO, RiceMillingCompleteDTO, RiceMillingBatchQuery } from '@/types/millingBatch'
import { mockMillingBatches } from '../mock/data/millingBatches'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post, put } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

async function mockGetList(query: RiceMillingBatchQuery): Promise<PageResponse<RiceMillingBatch>> {
  await mockDelay()
  let list = [...mockMillingBatches]

  if (query.grainBatchId) list = list.filter((m) => m.grainBatchId.includes(query.grainBatchId!))
  if (query.productBatchId) list = list.filter((m) => m.productBatchId.includes(query.productBatchId!))
  if (query.chainStatus) list = list.filter((m) => m.chainStatus === query.chainStatus)

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockGetById(id: string): Promise<ApiResponse<RiceMillingBatch>> {
  await mockDelay()
  const batch = mockMillingBatches.find((m) => m.millingBatchId === id)
  if (!batch) return { code: 404, message: '加工批次不存在', data: null as unknown as RiceMillingBatch }
  return { code: 200, message: 'success', data: batch }
}

async function mockCreate(dto: RiceMillingBatchCreateDTO): Promise<ApiResponse<RiceMillingBatch>> {
  await mockDelay()
  const newBatch: RiceMillingBatch = {
    ...dto,
    millingBatchId: mockId('MB'),
    tenantId: 'TENANT001',
    chainStatus: 'PENDING' as never,
    createdBy: 'FACTORY_STAFF001',
    createdAt: new Date().toISOString(),
  }
  mockMillingBatches.unshift(newBatch)
  return { code: 200, message: '创建成功', data: newBatch }
}

async function mockComplete(dto: RiceMillingCompleteDTO): Promise<ApiResponse<RiceMillingBatch>> {
  await mockDelay()
  const idx = mockMillingBatches.findIndex((m) => m.millingBatchId === dto.millingBatchId)
  if (idx === -1) return { code: 404, message: '加工批次不存在', data: null as unknown as RiceMillingBatch }

  const batch = mockMillingBatches[idx]
  const yieldRate = dto.riceOutputWeightKg / batch.grainOutWeightKg * 100
  mockMillingBatches[idx] = {
    ...batch,
    ...dto,
    yieldRate: Math.round(yieldRate * 100) / 100,
    chainStatus: 'PENDING' as never,
    updatedAt: new Date().toISOString(),
  }
  return { code: 200, message: '完成加工成功', data: mockMillingBatches[idx] }
}

// Real API stubs
async function realGetList(query: RiceMillingBatchQuery): Promise<PageResponse<RiceMillingBatch>> {
  const res = await get<PageResponse<RiceMillingBatch>['data']>('/rice/milling-batches', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceMillingBatch>
}

async function realGetById(id: string): Promise<ApiResponse<RiceMillingBatch>> {
  const res = await get<RiceMillingBatch>(`/rice/milling-batches/${id}`)
  return res.data as ApiResponse<RiceMillingBatch>
}

async function realCreate(dto: RiceMillingBatchCreateDTO): Promise<ApiResponse<RiceMillingBatch>> {
  const res = await post<RiceMillingBatch>('/rice/milling-batches', dto)
  return res.data as ApiResponse<RiceMillingBatch>
}

async function realComplete(dto: RiceMillingCompleteDTO): Promise<ApiResponse<RiceMillingBatch>> {
  const res = await put<RiceMillingBatch>(`/rice/milling-batches/${dto.millingBatchId}/complete`, dto)
  return res.data as ApiResponse<RiceMillingBatch>
}

export const millingBatchApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  getById: USE_MOCK ? mockGetById : realGetById,
  create: USE_MOCK ? mockCreate : realCreate,
  complete: USE_MOCK ? mockComplete : realComplete,
}
