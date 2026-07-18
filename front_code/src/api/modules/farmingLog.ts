/**
 * 农事记录 API 模块
 */
import type { ApiResponse, PageResponse } from '@/types/api'
import type { RiceFarmingLog, RiceFarmingLogCreateDTO, RiceFarmingLogQuery } from '@/types/farmingLog'
import { mockFarmingLogs } from '../mock/data/farmingLogs'
import { mockDelay, mockPaginate, mockId } from '../mock'
import { get, post } from '../request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

async function mockGetList(query: RiceFarmingLogQuery): Promise<PageResponse<RiceFarmingLog>> {
  await mockDelay()
  let list = [...mockFarmingLogs]

  if (query.plantingBatchId) list = list.filter((l) => l.plantingBatchId === query.plantingBatchId)
  if (query.operationType) list = list.filter((l) => l.operationType === query.operationType)

  // 按时间倒序
  list.sort((a, b) => new Date(b.operationTime).getTime() - new Date(a.operationTime).getTime())

  const data = mockPaginate(list, query.page || 1, query.pageSize || 10)
  return { code: 200, message: 'success', data }
}

async function mockCreate(dto: RiceFarmingLogCreateDTO): Promise<ApiResponse<RiceFarmingLog>> {
  await mockDelay()
  const newLog: RiceFarmingLog = {
    ...dto,
    logId: mockId('FLOG'),
    tenantId: 'TENANT001',
    chainStatus: 'PENDING' as never,
    createdBy: dto.operatorId,
    createdAt: new Date().toISOString(),
  }
  mockFarmingLogs.unshift(newLog)
  return { code: 200, message: '创建成功', data: newLog }
}

// Real API stubs
async function realGetList(query: RiceFarmingLogQuery): Promise<PageResponse<RiceFarmingLog>> {
  const res = await get<PageResponse<RiceFarmingLog>['data']>('/rice/farming-logs', query as Record<string, unknown>)
  return res.data as unknown as PageResponse<RiceFarmingLog>
}

async function realCreate(dto: RiceFarmingLogCreateDTO): Promise<ApiResponse<RiceFarmingLog>> {
  const res = await post<RiceFarmingLog>('/rice/farming-logs', dto)
  return res.data as ApiResponse<RiceFarmingLog>
}

export const farmingLogApi = {
  getList: USE_MOCK ? mockGetList : realGetList,
  create: USE_MOCK ? mockCreate : realCreate,
}
