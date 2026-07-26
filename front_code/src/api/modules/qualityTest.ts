/**
 * 质检 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceQualityTest, RiceQualityTestCreateDTO } from '@/types/qualityTest'
import { mockQualityTests } from '../mock/data/qualityTests'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

async function mockGetByBusiness(businessType: string, businessId: string): Promise<ApiResponse<RiceQualityTest | null>> {
  await mockDelay()
  const test = mockQualityTests.find((t) => t.businessType === businessType && t.businessId === businessId)
  return { code: 200, message: 'success', data: test || null }
}

async function mockGetList(query: { businessType?: string; businessId?: string; page?: number; pageSize?: number }): Promise<PageResponse<RiceQualityTest>> {
  await mockDelay()
  let list = [...mockQualityTests]
  if (query.businessType) list = list.filter((t) => t.businessType === query.businessType)
  if (query.businessId) list = list.filter((t) => t.businessId === query.businessId)
  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockCreate(dto: RiceQualityTestCreateDTO): Promise<ApiResponse<RiceQualityTest>> {
  await mockDelay()
  const newTest: RiceQualityTest = {
    ...dto,
    qualityTestId: mockId('QTEST'),
    tenantId: 'TENANT001',
    chainStatus: 'PENDING' as never,
    createdBy: 'TESTER001',
    createdAt: new Date().toISOString(),
    items: dto.items.map((item, idx) => ({
      ...item,
      itemId: `ITEM_MOCK_${Date.now()}_${idx}`,
      qualityTestId: mockId('QTEST'),
    })),
  }
  mockQualityTests.unshift(newTest)
  return { code: 200, message: '创建成功', data: newTest }
}

// Real API stubs
async function realGetByBusiness(businessType: string, businessId: string): Promise<ApiResponse<RiceQualityTest | null>> {
  const res = await get<RiceQualityTest | null>(`/rice/quality-tests`, { businessType, businessId })
  return res.data as ApiResponse<RiceQualityTest | null>
}

async function realGetList(query: { businessType?: string; businessId?: string; page?: number; pageSize?: number }): Promise<PageResponse<RiceQualityTest>> {
  const res = await get<PageResponse<RiceQualityTest>['data']>('/rice/quality-tests', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceQualityTest>
}

async function realCreate(dto: RiceQualityTestCreateDTO): Promise<ApiResponse<RiceQualityTest>> {
  const res = await post<RiceQualityTest>('/rice/quality-tests', dto)
  return res.data as ApiResponse<RiceQualityTest>
}

export const qualityTestApi = {
  getByBusiness: USE_MOCK ? mockGetByBusiness : realGetByBusiness,
  getList: USE_MOCK ? mockGetList : realGetList,
  create: USE_MOCK ? mockCreate : realCreate,
}
