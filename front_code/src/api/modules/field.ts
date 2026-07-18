/**
 * 地块 API 模块
 * 通过 VITE_USE_MOCK 环境变量控制使用 Mock 还是真实接口
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceField, RiceFieldCreateDTO, RiceFieldUpdateDTO, RiceFieldQuery } from '@/types/field'
import { mockFields } from '../mock/data/fields'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post, put, del } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// ==================== Mock 实现 ====================
async function mockGetList(query: RiceFieldQuery): Promise<PageResponse<RiceField>> {
  await mockDelay()
  let list = [...mockFields]

  if (query.fieldCode) list = list.filter((f) => f.fieldCode.includes(query.fieldCode!))
  if (query.fieldName) list = list.filter((f) => f.fieldName.includes(query.fieldName!))
  if (query.farmerName) list = list.filter((f) => f.farmerName.includes(query.farmerName!))
  if (query.province) list = list.filter((f) => f.province === query.province)
  if (query.chainStatus) list = list.filter((f) => f.chainStatus === query.chainStatus)

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockGetById(fieldId: string): Promise<ApiResponse<RiceField>> {
  await mockDelay()
  const field = mockFields.find((f) => f.fieldId === fieldId)
  if (!field) return { code: 404, message: '地块不存在', data: null as unknown as RiceField }
  return { code: 200, message: 'success', data: field }
}

async function mockCreate(dto: RiceFieldCreateDTO): Promise<ApiResponse<RiceField>> {
  await mockDelay()
  const newField: RiceField = {
    ...dto,
    fieldId: mockId('FIELD'),
    tenantId: 'TENANT001',
    coordinateHash: 'sha256:mock_' + Date.now(),
    chainStatus: 'PENDING' as never,
    createdBy: 'ADMIN001',
    createdAt: new Date().toISOString(),
  }
  mockFields.unshift(newField)
  return { code: 200, message: '创建成功', data: newField }
}

async function mockUpdate(dto: RiceFieldUpdateDTO): Promise<ApiResponse<RiceField>> {
  await mockDelay()
  const idx = mockFields.findIndex((f) => f.fieldId === dto.fieldId)
  if (idx === -1) return { code: 404, message: '地块不存在', data: null as unknown as RiceField }
  mockFields[idx] = { ...mockFields[idx], ...dto, updatedAt: new Date().toISOString() }
  return { code: 200, message: '更新成功', data: mockFields[idx] }
}

// ==================== 真实 API 实现 ====================
async function realGetList(query: RiceFieldQuery): Promise<PageResponse<RiceField>> {
  const res = await get<PageResponse<RiceField>['data']>('/rice/fields', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceField>
}

async function realGetById(fieldId: string): Promise<ApiResponse<RiceField>> {
  const res = await get<RiceField>(`/rice/fields/${fieldId}`)
  return res.data as ApiResponse<RiceField>
}

async function realCreate(dto: RiceFieldCreateDTO): Promise<ApiResponse<RiceField>> {
  const res = await post<RiceField>('/rice/fields', dto)
  return res.data as ApiResponse<RiceField>
}

async function realUpdate(dto: RiceFieldUpdateDTO): Promise<ApiResponse<RiceField>> {
  const res = await put<RiceField>(`/rice/fields/${dto.fieldId}`, dto)
  return res.data as ApiResponse<RiceField>
}

async function realDelete(fieldId: string): Promise<ApiResponse<null>> {
  const res = await del<null>(`/rice/fields/${fieldId}`)
  return res.data as ApiResponse<null>
}

// ==================== 导出 ====================
export const fieldApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  getById: USE_MOCK ? mockGetById : realGetById,
  create: USE_MOCK ? mockCreate : realCreate,
  update: USE_MOCK ? mockUpdate : realUpdate,
  delete: USE_MOCK ? async (fieldId: string) => { await mockDelay(); return { code: 200, message: '删除成功', data: null } } : realDelete,
}
