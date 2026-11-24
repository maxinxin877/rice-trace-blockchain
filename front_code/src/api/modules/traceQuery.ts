/**
 * 追溯查询 API（后端 RiceTraceQueryController）
 */
import type { ApiResponse } from '@/types/api'
import type { TraceChain, TraceImpact, TraceResolveResult, TraceSearchResult } from '@/types/traceQuery'
import { get } from '../request'

export const traceQueryApi = {
  /** 识别关键词类型 */
  async resolve(keyword: string): Promise<ApiResponse<TraceResolveResult>> {
    return (await get<TraceResolveResult>('/rice/trace/resolve', { keyword })).data
  },

  /** 一站式检索：识别 + 链路 */
  async search(keyword: string): Promise<ApiResponse<TraceSearchResult>> {
    return (await get<TraceSearchResult>('/rice/trace/search', { keyword })).data
  },

  /** 链路详情 */
  async chain(businessType: string, businessId: string, direction = 'BOTH'): Promise<ApiResponse<TraceChain>> {
    return (await get<TraceChain>('/rice/trace/chain', { businessType, businessId, direction })).data
  },

  /** 影响范围分析 */
  async impact(businessType: string, businessId: string): Promise<ApiResponse<TraceImpact>> {
    return (await get<TraceImpact>('/rice/trace/impact', { businessType, businessId })).data
  },
}
