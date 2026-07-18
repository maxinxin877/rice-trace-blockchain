/**
 * 收储入库 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceStorageReceipt, RiceStorageReceiptCreateDTO, RiceStorageReceiptQuery } from '@/types/storageReceipt'
import { mockStorageReceipts } from '../mock/data/storageReceipts'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

async function mockGetList(query: RiceStorageReceiptQuery): Promise<PageResponse<RiceStorageReceipt>> {
  await mockDelay()
  let list = [...mockStorageReceipts]

  if (query.plantingBatchId) list = list.filter((r) => r.plantingBatchId === query.plantingBatchId)
  if (query.grainBatchId) list = list.filter((r) => r.grainBatchId.includes(query.grainBatchId!))
  if (query.chainStatus) list = list.filter((r) => r.chainStatus === query.chainStatus)

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockGetById(id: string): Promise<ApiResponse<RiceStorageReceipt>> {
  await mockDelay()
  const receipt = mockStorageReceipts.find((r) => r.storageReceiptId === id)
  if (!receipt) return { code: 404, message: '入库单不存在', data: null as unknown as RiceStorageReceipt }
  return { code: 200, message: 'success', data: receipt }
}

async function mockCreate(dto: RiceStorageReceiptCreateDTO): Promise<ApiResponse<RiceStorageReceipt>> {
  await mockDelay()
  const newReceipt: RiceStorageReceipt = {
    ...dto,
    storageReceiptId: mockId('SREC'),
    tenantId: 'TENANT001',
    chainStatus: 'PENDING' as never,
    createdBy: 'WH_STAFF001',
    createdAt: new Date().toISOString(),
  }
  mockStorageReceipts.unshift(newReceipt)
  return { code: 200, message: '创建成功', data: newReceipt }
}

// Real API stubs
async function realGetList(query: RiceStorageReceiptQuery): Promise<PageResponse<RiceStorageReceipt>> {
  const res = await get<PageResponse<RiceStorageReceipt>['data']>('/rice/storage-receipts', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceStorageReceipt>
}

async function realGetById(id: string): Promise<ApiResponse<RiceStorageReceipt>> {
  const res = await get<RiceStorageReceipt>(`/rice/storage-receipts/${id}`)
  return res.data as ApiResponse<RiceStorageReceipt>
}

async function realCreate(dto: RiceStorageReceiptCreateDTO): Promise<ApiResponse<RiceStorageReceipt>> {
  const res = await post<RiceStorageReceipt>('/rice/storage-receipts', dto)
  return res.data as ApiResponse<RiceStorageReceipt>
}

export const storageReceiptApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  getById: USE_MOCK ? mockGetById : realGetById,
  create: USE_MOCK ? mockCreate : realCreate,
}
