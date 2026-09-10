/**
 * 质检 API 模块
 */
import type { ApiResponse } from '@/types/api'
import type { RiceQualityTest, RiceQualityTestCreateDTO } from '@/types/qualityTest'
import { mockQualityTests } from '../mock/data/qualityTests'
import { mockDelay, mockId } from '../mock'
import { post } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

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

async function realCreate(dto: RiceQualityTestCreateDTO): Promise<ApiResponse<RiceQualityTest>> {
  // 后端质检接口为入库单下的嵌套路径，且字段为 testItems / value
  const payload = {
    testAgency: dto.testAgency,
    testTime: dto.testTime,
    overallResult: dto.overallResult,
    reportFileId: dto.reportFileId,
    remark: dto.remark,
    testItems: (dto.items || []).map((item) => ({
      itemName: item.itemName,
      value: item.itemValue,
      unit: item.unit,
      standardValue: item.standardValue,
      result: item.result,
    })),
  }
  const res = await post<RiceQualityTest>(`/rice/storage-receipts/${dto.businessId}/quality-tests`, payload)
  return res.data as ApiResponse<RiceQualityTest>
}

export const qualityTestApi = {
  create: USE_MOCK ? mockCreate : realCreate,
}
