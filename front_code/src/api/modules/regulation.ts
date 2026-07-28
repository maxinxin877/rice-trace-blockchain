import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  AuditLogQuery,
  AuditLogRecord,
  HandleRiskWarningPayload,
  HandleRiskWarningResult,
  RiskWarningQuery,
  RiskWarningRecord,
  YieldBalanceCheckPayload,
  YieldBalanceQuery,
  YieldBalanceResult,
} from '@/types/regulation'
import { mockDelay, mockPaginate } from '@/api/mock'
import { auditLogs, riskWarnings, yieldBalanceResults } from '@/api/mock/data/regulation'
import { get, post, put } from '@/api/request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

function ok<T>(data: T, message = 'success'): ApiResponse<T> {
  return { code: 200, message, data }
}

function mockNow() {
  return '2026-07-26 11:45:00'
}

function pageParams(query: { page?: number; pageSize?: number }) {
  const { page, ...rest } = query
  return { ...rest, pageNo: page || 1, pageSize: query.pageSize || 10 }
}

export const regulationApi = {
  async checkYieldBalance(payload: YieldBalanceCheckPayload): Promise<ApiResponse<YieldBalanceResult>> {
    if (!USE_MOCK) return (await post<YieldBalanceResult>('/rice/regulation/yield-balance/check', payload)).data
    await mockDelay(600)
    const matched = yieldBalanceResults.find((item) =>
      (!payload.plantingBatchId || item.plantingBatchId === payload.plantingBatchId)
      && (!payload.grainBatchId || item.grainBatchId === payload.grainBatchId)
      && (!payload.productBatchId || item.productBatchId === payload.productBatchId),
    ) || yieldBalanceResults[0]!
    const result = { ...matched, ...payload, checkId: `YBC20260726${String(yieldBalanceResults.length + 1).padStart(4, '0')}`, checkedAt: mockNow() }
    yieldBalanceResults.unshift(result)
    return ok(result, '校验完成')
  },

  async getYieldBalanceResults(query: YieldBalanceQuery = {}): Promise<PageResponse<YieldBalanceResult>> {
    if (!USE_MOCK) return (await get<PageResponse<YieldBalanceResult>['data']>('/rice/regulation/yield-balance/results', pageParams(query))).data
    await mockDelay()
    let list = [...yieldBalanceResults]
    if (query.checkId) list = list.filter((item) => item.checkId.includes(query.checkId!))
    if (query.plantingBatchId) list = list.filter((item) => item.plantingBatchId?.includes(query.plantingBatchId!))
    if (query.grainBatchId) list = list.filter((item) => item.grainBatchId?.includes(query.grainBatchId!))
    if (query.productBatchId) list = list.filter((item) => item.productBatchId?.includes(query.productBatchId!))
    if (query.result) list = list.filter((item) => item.result === query.result)
    return ok(mockPaginate(list, query.page || 1, query.pageSize || 10))
  },

  async getRiskWarnings(query: RiskWarningQuery = {}): Promise<PageResponse<RiskWarningRecord>> {
    if (!USE_MOCK) return (await get<PageResponse<RiskWarningRecord>['data']>('/rice/regulation/risk-warnings', pageParams(query))).data
    await mockDelay()
    let list = [...riskWarnings]
    if (query.businessId) list = list.filter((item) => item.businessId.includes(query.businessId!))
    if (query.warningType) list = list.filter((item) => item.warningType === query.warningType)
    if (query.riskLevel) list = list.filter((item) => item.riskLevel === query.riskLevel)
    if (typeof query.handled === 'boolean') list = list.filter((item) => item.handled === query.handled)
    return ok(mockPaginate(list, query.page || 1, query.pageSize || 10))
  },

  async handleRiskWarning(warningId: string, payload: HandleRiskWarningPayload): Promise<ApiResponse<HandleRiskWarningResult>> {
    if (!USE_MOCK) return (await put<HandleRiskWarningResult>(`/rice/regulation/risk-warnings/${warningId}/handle`, payload)).data
    await mockDelay(400)
    const warning = riskWarnings.find((item) => item.warningId === warningId)
    if (!warning) return { code: 404, message: '风险预警不存在', data: null as unknown as HandleRiskWarningResult }
    if (warning.handled) return { code: 409, message: '该风险预警已处理', data: null as unknown as HandleRiskWarningResult }
    const handledAt = mockNow()
    warning.handled = true
    warning.handledBy = 'ADMIN001'
    warning.handledAt = handledAt
    warning.handleResult = payload.handleResult
    warning.chainStatus = 'PENDING'
    auditLogs.unshift({ auditId: `AUDIT20260726${String(auditLogs.length + 1).padStart(4, '0')}`, businessType: warning.businessType, businessId: warning.businessId, operationType: 'HANDLE', operatorId: 'ADMIN001', operatorName: '系统管理员', beforeHash: null, afterHash: `sha256:${warning.warningId.toLowerCase()}`, reason: payload.reason, operationTime: handledAt, txId: null })
    return ok({ warningId, handled: true, handledBy: 'ADMIN001', handledAt, chainStatus: 'PENDING' }, '处理完成')
  },

  async getAuditLogs(query: AuditLogQuery = {}): Promise<PageResponse<AuditLogRecord>> {
    if (!USE_MOCK) return (await get<PageResponse<AuditLogRecord>['data']>('/rice/regulation/audit-logs', pageParams(query))).data
    await mockDelay()
    let list = [...auditLogs]
    if (query.businessType) list = list.filter((item) => item.businessType === query.businessType)
    if (query.businessId) list = list.filter((item) => item.businessId.includes(query.businessId!))
    if (query.operatorId) list = list.filter((item) => item.operatorId.includes(query.operatorId!))
    if (query.operationType) list = list.filter((item) => item.operationType === query.operationType)
    return ok(mockPaginate(list, query.page || 1, query.pageSize || 10))
  },
}
