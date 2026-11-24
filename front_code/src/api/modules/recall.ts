/**
 * 召回管理 API（后端 RiceRecallController）
 */
import type { ApiResponse } from '@/types/api'
import type { RecallCreatePayload, RecallItem, RecallTask } from '@/types/recall'
import type { TraceImpact } from '@/types/traceQuery'
import { get, post } from '../request'

export const recallApi = {
  /** 影响范围预览 */
  async previewScope(businessType: string, businessId: string): Promise<ApiResponse<TraceImpact>> {
    return (await get<TraceImpact>('/rice/recalls/scope-preview', { businessType, businessId })).data
  },

  /** 创建召回任务 */
  async create(payload: RecallCreatePayload): Promise<ApiResponse<RecallTask>> {
    return (await post<RecallTask>('/rice/recalls', payload)).data
  },

  /** 提交审批 */
  async submit(recallTaskId: string): Promise<ApiResponse<RecallTask>> {
    return (await post<RecallTask>(`/rice/recalls/${recallTaskId}/submit`)).data
  },

  /** 审批通过 */
  async approve(recallTaskId: string, comment: string): Promise<ApiResponse<RecallTask>> {
    return (await post<RecallTask>(`/rice/recalls/${recallTaskId}/approve`, { comment })).data
  },

  /** 审批驳回 */
  async reject(recallTaskId: string, comment: string): Promise<ApiResponse<RecallTask>> {
    return (await post<RecallTask>(`/rice/recalls/${recallTaskId}/reject`, { comment })).data
  },

  /** 发布召回（冻结 + 通知 + 上链） */
  async release(recallTaskId: string, notifyChannels: string[] = ['SMS', 'WECHAT']): Promise<ApiResponse<RecallTask>> {
    return (await post<RecallTask>(`/rice/recalls/${recallTaskId}/release`, { notifyChannels })).data
  },

  /** 登记明细处置结果 */
  async handleItem(itemId: string, handleStatus: string, remark: string): Promise<ApiResponse<RecallItem>> {
    return (await post<RecallItem>(`/rice/recalls/items/${itemId}/handle`, { handleStatus, remark })).data
  },

  /** 关闭召回 */
  async close(recallTaskId: string, closeResult: string): Promise<ApiResponse<RecallTask>> {
    return (await post<RecallTask>(`/rice/recalls/${recallTaskId}/close`, { closeResult })).data
  },

  /** 取消召回 */
  async cancel(recallTaskId: string, reason: string): Promise<ApiResponse<RecallTask>> {
    return (await post<RecallTask>(`/rice/recalls/${recallTaskId}/cancel`, undefined, {
      params: { reason },
    })).data
  },

  /** 分页查询 */
  async getList(params: Record<string, unknown>) {
    return (await get<{ records: RecallTask[]; total: number }>('/rice/recalls', params)).data
  },

  /** 详情 */
  async getById(recallTaskId: string): Promise<ApiResponse<RecallTask>> {
    return (await get<RecallTask>(`/rice/recalls/${recallTaskId}`)).data
  },
}
