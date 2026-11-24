/**
 * 扫码采集 API（后端 RiceCollectController）
 */
import type { ApiResponse } from '@/types/api'
import type { CollectPayload, CollectRecord, CollectStats } from '@/types/collect'
import { get, post } from '../request'

export const collectApi = {
  /** 单条采集 */
  async collect(payload: CollectPayload): Promise<ApiResponse<CollectRecord>> {
    return (await post<CollectRecord>('/rice/collect', payload)).data
  },

  /** 离线补传 */
  async batchCollect(records: CollectPayload[]): Promise<ApiResponse<Record<string, unknown>>> {
    return (await post<Record<string, unknown>>('/rice/collect/batch', { records })).data
  },

  /** 补录审核 */
  async review(collectId: string, approved: boolean, comment: string): Promise<ApiResponse<CollectRecord>> {
    return (await post<CollectRecord>(`/rice/collect/${collectId}/review`, { approved, comment })).data
  },

  /** 分页查询 */
  async getList(params: Record<string, unknown>) {
    return (await get<{ records: CollectRecord[]; total: number }>('/rice/collect', params)).data
  },

  /** 概览 */
  async stats(): Promise<ApiResponse<CollectStats>> {
    return (await get<CollectStats>('/rice/collect/stats')).data
  },
}
