/**
 * 种植批次 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RicePlantingBatch, RicePlantingBatchCreateDTO, RicePlantingBatchQuery } from '@/types/plantingBatch'
import { mockPlantingBatches } from '../mock/data/plantingBatches'
import { mockFields } from '../mock/data/fields'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post, put } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

async function mockGetList(query: RicePlantingBatchQuery): Promise<PageResponse<RicePlantingBatch>> {
  await mockDelay()
  let list = [...mockPlantingBatches]

  if (query.fieldId) list = list.filter((b) => b.fieldId === query.fieldId)
  if (query.riceVariety) list = list.filter((b) => b.riceVariety.includes(query.riceVariety!))
  if (query.status) list = list.filter((b) => b.status === query.status)

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockGetById(batchId: string): Promise<ApiResponse<RicePlantingBatch>> {
  await mockDelay()
  const batch = mockPlantingBatches.find((b) => b.plantingBatchId === batchId)
  if (!batch) return { code: 404, message: '种植批次不存在', data: null as unknown as RicePlantingBatch }
  return { code: 200, message: 'success', data: batch }
}

async function mockCreate(dto: RicePlantingBatchCreateDTO): Promise<ApiResponse<RicePlantingBatch>> {
  await mockDelay()
  const field = mockFields.find((f) => f.fieldId === dto.fieldId)
  const newBatch: RicePlantingBatch = {
    ...dto,
    plantingBatchId: mockId('RPB'),
    tenantId: 'TENANT001',
    fieldCode: field?.fieldCode,
    fieldName: field?.fieldName,
    status: 'PLANTED' as never,
    chainStatus: 'PENDING' as never,
    createdBy: 'ADMIN001',
    createdAt: new Date().toISOString(),
  }
  mockPlantingBatches.unshift(newBatch)
  return { code: 200, message: '创建成功', data: newBatch }
}

async function mockUpdate(batchId: string, dto: Partial<RicePlantingBatchCreateDTO>): Promise<ApiResponse<RicePlantingBatch>> {
  await mockDelay()
  const idx = mockPlantingBatches.findIndex((b) => b.plantingBatchId === batchId)
  if (idx === -1) return { code: 404, message: '种植批次不存在', data: null as unknown as RicePlantingBatch }
  mockPlantingBatches[idx] = { ...mockPlantingBatches[idx], ...dto, updatedAt: new Date().toISOString() }
  return { code: 200, message: '更新成功', data: mockPlantingBatches[idx] }
}

// Real API stubs (for future use)
async function realGetList(query: RicePlantingBatchQuery): Promise<PageResponse<RicePlantingBatch>> {
  const res = await get<PageResponse<RicePlantingBatch>['data']>('/rice/planting-batches', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RicePlantingBatch>
}

async function realGetById(batchId: string): Promise<ApiResponse<RicePlantingBatch>> {
  const res = await get<RicePlantingBatch>(`/rice/planting-batches/${batchId}`)
  return res.data as ApiResponse<RicePlantingBatch>
}

async function realCreate(dto: RicePlantingBatchCreateDTO): Promise<ApiResponse<RicePlantingBatch>> {
  const res = await post<RicePlantingBatch>('/rice/planting-batches', dto)
  return res.data as ApiResponse<RicePlantingBatch>
}

async function realUpdate(batchId: string, dto: Partial<RicePlantingBatchCreateDTO>): Promise<ApiResponse<RicePlantingBatch>> {
  const res = await put<RicePlantingBatch>(`/rice/planting-batches/${batchId}`, dto)
  return res.data as ApiResponse<RicePlantingBatch>
}

export const plantingBatchApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  getById: USE_MOCK ? mockGetById : realGetById,
  create: USE_MOCK ? mockCreate : realCreate,
  update: USE_MOCK ? mockUpdate : realUpdate,
}
