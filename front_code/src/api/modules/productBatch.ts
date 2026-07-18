/**
 * 成品批次 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceProductBatch, RiceProductBatchCreateDTO, RiceProductBatchUpdateDTO, RiceProductBatchQuery } from '@/types/productBatch'
import { mockProductBatches } from '../mock/data/productBatches'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post, put } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

async function mockGetList(query: RiceProductBatchQuery): Promise<PageResponse<RiceProductBatch>> {
  await mockDelay()
  let list = [...mockProductBatches]

  if (query.productName) list = list.filter((p) => p.productName.includes(query.productName!))
  if (query.brandName) list = list.filter((p) => p.brandName.includes(query.brandName!))
  if (query.riceVariety) list = list.filter((p) => p.riceVariety.includes(query.riceVariety!))
  if (query.status) list = list.filter((p) => p.status === query.status)

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockGetById(id: string): Promise<ApiResponse<RiceProductBatch>> {
  await mockDelay()
  const batch = mockProductBatches.find((p) => p.productBatchId === id)
  if (!batch) return { code: 404, message: '成品批次不存在', data: null as unknown as RiceProductBatch }
  return { code: 200, message: 'success', data: batch }
}

async function mockCreate(dto: RiceProductBatchCreateDTO): Promise<ApiResponse<RiceProductBatch>> {
  await mockDelay()
  const newBatch: RiceProductBatch = {
    ...dto,
    tenantId: 'TENANT001',
    status: 'PENDING',
    createdBy: 'BRAND_STAFF001',
    createdAt: new Date().toISOString(),
  }
  mockProductBatches.unshift(newBatch)
  return { code: 200, message: '创建成功', data: newBatch }
}

async function mockUpdate(id: string, dto: RiceProductBatchUpdateDTO): Promise<ApiResponse<RiceProductBatch>> {
  await mockDelay()
  const idx = mockProductBatches.findIndex((p) => p.productBatchId === id)
  if (idx === -1) return { code: 404, message: '成品批次不存在', data: null as unknown as RiceProductBatch }
  mockProductBatches[idx] = { ...mockProductBatches[idx], ...dto, updatedAt: new Date().toISOString() }
  return { code: 200, message: '更新成功', data: mockProductBatches[idx] }
}

// Real API stubs
async function realGetList(query: RiceProductBatchQuery): Promise<PageResponse<RiceProductBatch>> {
  const res = await get<PageResponse<RiceProductBatch>['data']>('/rice/product-batches', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceProductBatch>
}

async function realGetById(id: string): Promise<ApiResponse<RiceProductBatch>> {
  const res = await get<RiceProductBatch>(`/rice/product-batches/${id}`)
  return res.data as ApiResponse<RiceProductBatch>
}

async function realCreate(dto: RiceProductBatchCreateDTO): Promise<ApiResponse<RiceProductBatch>> {
  const res = await post<RiceProductBatch>('/rice/product-batches', dto)
  return res.data as ApiResponse<RiceProductBatch>
}

async function realUpdate(id: string, dto: RiceProductBatchUpdateDTO): Promise<ApiResponse<RiceProductBatch>> {
  const res = await put<RiceProductBatch>(`/rice/product-batches/${id}`, dto)
  return res.data as ApiResponse<RiceProductBatch>
}

export const productBatchApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  getById: USE_MOCK ? mockGetById : realGetById,
  create: USE_MOCK ? mockCreate : realCreate,
  update: USE_MOCK ? mockUpdate : realUpdate,
}
