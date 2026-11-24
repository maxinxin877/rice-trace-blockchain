/**
 * 不合格品处置 API（后端 RiceNonconformingDisposalController）
 */
import type { ApiResponse } from '@/types/api'
import type { Disposal, DisposalPayload } from '@/types/disposal'
import { get, post } from '../request'

export const disposalApi = {
  async create(payload: DisposalPayload): Promise<ApiResponse<Disposal>> {
    return (await post<Disposal>('/rice/quality/disposals', payload)).data
  },

  async submit(disposalId: string): Promise<ApiResponse<Disposal>> {
    return (await post<Disposal>(`/rice/quality/disposals/${disposalId}/submit`)).data
  },

  async review(disposalId: string, approved: boolean, comment: string): Promise<ApiResponse<Disposal>> {
    return (await post<Disposal>(`/rice/quality/disposals/${disposalId}/review`, { approved, comment })).data
  },

  async execute(disposalId: string, remark: string): Promise<ApiResponse<Disposal>> {
    return (await post<Disposal>(`/rice/quality/disposals/${disposalId}/execute`, { remark })).data
  },

  async close(disposalId: string, remark: string): Promise<ApiResponse<Disposal>> {
    return (await post<Disposal>(`/rice/quality/disposals/${disposalId}/close`, { remark })).data
  },

  async getList(params: Record<string, unknown>) {
    return (await get<{ records: Disposal[]; total: number }>('/rice/quality/disposals', params)).data
  },

  async getById(disposalId: string): Promise<ApiResponse<Disposal>> {
    return (await get<Disposal>(`/rice/quality/disposals/${disposalId}`)).data
  },
}
