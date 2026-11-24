/** 扫码采集类型（对应后端 /rice/collect/**） */

export type CollectAction = 'STORE' | 'MILL' | 'ACTIVATE' | 'OUTBOUND' | 'SHELF' | 'SELL' | 'INSPECT'

export interface CollectRecord {
  collectId: string
  idempotencyKey: string
  collectCode: string
  codeType: string
  action: CollectAction
  terminalType: string
  operatorName: string
  collectTime: string
  source: 'ONLINE' | 'OFFLINE' | 'MANUAL'
  businessType: string
  businessId: string
  snapshot?: Record<string, unknown>
  checkResult: 'PASS' | 'WARN' | 'BLOCK'
  checkMessage: string
  duplicated: boolean
  status: 'COLLECTED' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'
  reviewReason: string
  reviewedBy?: string
  reviewedAt?: string
  reviewComment: string
  createTime?: string
}

export interface CollectPayload {
  code: string
  action: CollectAction
  terminalType: string
  idempotencyKey?: string
  collectTime?: string
  source?: 'ONLINE' | 'OFFLINE' | 'MANUAL'
  reason?: string
  operatorName?: string
}

export interface CollectStats {
  todayTotal: number
  todayBlocked: number
  pendingReview: number
  offlineTotal: number
  actionDistribution: { action: string; count: number }[]
}

export const COLLECT_ACTION_MAP: Record<string, string> = {
  STORE: '入库收储',
  MILL: '碾米加工',
  ACTIVATE: '赋码激活',
  OUTBOUND: '出库',
  SHELF: '上架',
  SELL: '销售',
  INSPECT: '巡检',
}

export const COLLECT_RESULT_MAP: Record<string, { label: string; color: string }> = {
  PASS: { label: '通过', color: '#67c23a' },
  WARN: { label: '告警', color: '#e6a23c' },
  BLOCK: { label: '拦截', color: '#f56c6c' },
}

export const COLLECT_TERMINAL_MAP: Record<string, string> = {
  SCANNER: '扫码枪',
  PDA: 'PDA',
  MOBILE: '移动端',
  WEB: '网页手工',
}

export const COLLECT_STATUS_MAP: Record<string, { label: string; color: string }> = {
  COLLECTED: { label: '已采集', color: '#909399' },
  PENDING_REVIEW: { label: '待审核', color: '#e6a23c' },
  APPROVED: { label: '审核通过', color: '#67c23a' },
  REJECTED: { label: '审核驳回', color: '#f56c6c' },
}

export const COLLECT_CODE_TYPE_MAP: Record<string, string> = {
  TRACE_CODE: '单品码',
  BOX_CODE: '箱码',
  PALLET_CODE: '托盘码',
  PRODUCT_BATCH: '成品批次',
  GRAIN_BATCH: '原粮批次',
  STORAGE_RECEIPT: '入库单',
  PLANTING_BATCH: '种植批次',
  UNKNOWN: '无法识别',
}
