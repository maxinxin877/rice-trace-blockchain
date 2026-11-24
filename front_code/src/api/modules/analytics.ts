/**
 * 质量分析 API（后端 RiceAnalyticsController）
 */
import type { ApiResponse } from '@/types/api'
import type { AnalyticsOverview } from '@/types/analytics'
import { get } from '../request'

export const analyticsApi = {
  async overview(): Promise<ApiResponse<AnalyticsOverview>> {
    return (await get<AnalyticsOverview>('/rice/analytics/overview')).data
  },
}
