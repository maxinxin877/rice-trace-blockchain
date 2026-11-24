/** 召回管理类型（对应后端 /rice/recalls/**） */

export type RecallStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'RELEASED'
  | 'HANDLING'
  | 'CLOSED'
  | 'REJECTED'
  | 'CANCELLED'

export type RecallRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export type RecallTriggerType =
  | 'QUALITY_FAIL'
  | 'CHANNEL_RISK'
  | 'COMPLAINT'
  | 'REGULATOR'
  | 'ENTERPRISE'

export type RecallItemType = 'PRODUCT_BATCH' | 'TRACE_CODE' | 'GRAIN_BATCH'

export type RecallHandleStatus =
  | 'PENDING'
  | 'OFF_SHELF'
  | 'RECOVERED'
  | 'ISOLATED'
  | 'RETURNED'
  | 'SCRAPPED'
  | 'UNRECOVERED'

export interface RecallProgress {
  total: number
  notifiedCount: number
  confirmedCount: number
  offShelfCount: number
  recoveredCount: number
  isolatedCount: number
  returnedCount: number
  scrappedCount: number
  unRecoveredCount: number
  notifyRate: number
  recoveryRate: number
  frozenCount: number
}

export interface RecallItem {
  itemId: string
  recallTaskId: string
  itemType: RecallItemType
  businessId: string
  productName: string
  batchNo: string
  packageSpec: string
  expectedRegion: string
  actualRegion: string
  quantity: number
  weightKg: number
  notifyStatus: string
  notifyChannel: string
  notifiedAt?: string
  handleStatus: RecallHandleStatus
  handleRemark: string
  handledBy: string
  handledAt?: string
  frozen: boolean
  createTime?: string
}

export interface RecallTask {
  recallTaskId: string
  recallNo: string
  triggerType: RecallTriggerType
  triggerBusinessType: string
  triggerBusinessId: string
  riskLevel: RecallRiskLevel
  recallReason: string
  affectedBatchCount: number
  affectedCodeCount: number
  affectedWeightKg: number
  affectedRegions: string
  status: RecallStatus
  deadline?: string
  initiatedBy: string
  initiatedAt?: string
  approvedBy: string
  approvedAt?: string
  approveComment: string
  releasedAt?: string
  closedAt?: string
  closeResult: string
  chainStatus: string
  txId: string
  blockHeight: number
  chainTime?: string
  chainError: string
  createTime?: string
  updateTime?: string
  items?: RecallItem[]
  progress?: RecallProgress
  triggerDetail?: Record<string, unknown>
}

export interface RecallQuery {
  page?: number
  pageSize?: number
  status?: string
  riskLevel?: string
  triggerType?: string
  keyword?: string
  startTime?: string
  endTime?: string
}

export interface RecallCreatePayload {
  triggerType: RecallTriggerType
  triggerBusinessType: string
  triggerBusinessId: string
  scopeBusinessType?: string
  scopeBusinessId?: string
  riskLevel?: RecallRiskLevel
  reason: string
  includeTraceCodes?: boolean
  maxTraceCodeItems?: number
  deadline?: string
}

/** 召回状态映射 */
export const RECALL_STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: '#909399' },
  SUBMITTED: { label: '待审批', color: '#e6a23c' },
  APPROVED: { label: '已批准', color: '#409eff' },
  RELEASED: { label: '已发布', color: '#c53030' },
  HANDLING: { label: '处置中', color: '#d97706' },
  CLOSED: { label: '已关闭', color: '#67c23a' },
  REJECTED: { label: '已驳回', color: '#909399' },
  CANCELLED: { label: '已取消', color: '#909399' },
}

/** 召回触发类型映射 */
export const RECALL_TRIGGER_MAP: Record<string, string> = {
  QUALITY_FAIL: '质检不合格',
  CHANNEL_RISK: '窜货风险',
  COMPLAINT: '投诉举报',
  REGULATOR: '监管指令',
  ENTERPRISE: '企业自查',
}

/** 明细处置状态映射 */
export const RECALL_HANDLE_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处置', color: '#909399' },
  OFF_SHELF: { label: '已下架', color: '#409eff' },
  RECOVERED: { label: '已回收', color: '#67c23a' },
  ISOLATED: { label: '已隔离', color: '#e6a23c' },
  RETURNED: { label: '已退货', color: '#7a5af8' },
  SCRAPPED: { label: '已销毁', color: '#c53030' },
  UNRECOVERED: { label: '未召回', color: '#f56c6c' },
}

/** 明细类型映射 */
export const RECALL_ITEM_TYPE_MAP: Record<string, string> = {
  PRODUCT_BATCH: '成品批次',
  TRACE_CODE: '防伪码',
  GRAIN_BATCH: '原粮批次',
}

/** 召回流程步骤 */
export const RECALL_STEPS = ['草稿', '提交审批', '审批通过', '发布冻结', '处置回收', '关闭归档']
