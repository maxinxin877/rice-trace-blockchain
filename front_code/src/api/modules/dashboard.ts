import type { ApiResponse } from '@/types/api'
import type { DashboardSummary } from '@/types/dashboard'
import { dashboardSummary } from '@/api/mock/data/dashboard'
import { mockDelay } from '@/api/mock'
import { get } from '@/api/request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export const dashboardApi = {
  async getSummary(): Promise<ApiResponse<DashboardSummary>> {
    if (!USE_MOCK) return (await get<DashboardSummary>('/rice/regulation/dashboard/summary')).data
    await mockDelay()
    return { code: 200, message: 'success', data: { ...dashboardSummary } }
  },
}
