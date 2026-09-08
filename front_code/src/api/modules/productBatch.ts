/**
 * 成品批次 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceProductBatch, RiceProductBatchCreateDTO, RiceProductBatchQuery, TraceResult } from '@/types/productBatch'
import { mockProductBatches } from '../mock/data/productBatches'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post } from '../request'

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

// Real API stubs
async function realGetList(query: RiceProductBatchQuery): Promise<PageResponse<RiceProductBatch>> {
  const res = await get<PageResponse<RiceProductBatch>['data']>('/rice/product-batches', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceProductBatch>
}

async function realCreate(dto: RiceProductBatchCreateDTO): Promise<ApiResponse<RiceProductBatch>> {
  const res = await post<RiceProductBatch>('/rice/product-batches', dto)
  return res.data as ApiResponse<RiceProductBatch>
}

async function mockTrace(productBatchId: string): Promise<ApiResponse<TraceResult>> {
  await mockDelay()
  const product = mockProductBatches.find((p) => p.productBatchId === productBatchId)
  return {
    code: 200,
    message: 'success',
    data: { productBatch: product || null, millingBatch: null, storageReceipt: null, plantingBatch: null, field: null, traceCodes: [] },
  }
}

async function realTrace(productBatchId: string): Promise<ApiResponse<TraceResult>> {
  const res = await get<TraceResult>(`/rice/product-batches/${productBatchId}/trace`)
  return res.data as ApiResponse<TraceResult>
}

export const productBatchApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  create: USE_MOCK ? mockCreate : realCreate,
  trace: USE_MOCK ? mockTrace : realTrace,
}
