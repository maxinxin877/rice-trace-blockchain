/** 不合格品处置类型（对应后端 /rice/quality/disposals/**） */

export type DisposalStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'EXECUTED'
  | 'CLOSED'

export interface Disposal {
  disposalId: string
  disposalNo: string
  sourceType: string
  sourceId: string
  businessType: string
  businessId: string
  batchNo: string
  productName: string
  defectType: string
  defectDesc: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  quantity: number
  weightKg: number
  disposition: string
  status: DisposalStatus
  isolated: boolean
  applicant: string
  appliedAt?: string
  reviewer: string
  reviewedAt?: string
  reviewComment: string
  executedAt?: string
  executeRemark: string
  chainStatus: string
  txId: string
  createTime?: string
}

export interface DisposalPayload {
  sourceType?: string
  sourceId?: string
  businessType: string
  businessId: string
  batchNo?: string
  productName?: string
  defectType: string
  defectDesc: string
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH'
  quantity?: number
  weightKg?: number
  disposition: string
}

export const DISPOSAL_STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: '#909399' },
  PENDING_REVIEW: { label: '待评审', color: '#e6a23c' },
  APPROVED: { label: '已通过(隔离)', color: '#c53030' },
  REJECTED: { label: '已驳回', color: '#909399' },
  EXECUTED: { label: '已执行', color: '#409eff' },
  CLOSED: { label: '已关闭', color: '#67c23a' },
}

export const DISPOSAL_METHOD_MAP: Record<string, { label: string; hint: string }> = {
  ISOLATE: { label: '隔离观察', hint: '继续隔离，禁止流转' },
  REWORK: { label: '返工', hint: '返工后重新检验，执行即解冻' },
  REPAIR: { label: '返修', hint: '返修后重新检验，执行即解冻' },
  SCRAP: { label: '报废销毁', hint: '产品报废，保持隔离' },
  CONCESSION: { label: '让步接收', hint: '降级接收，执行即解冻' },
  RELEASE: { label: '解除隔离放行', hint: '评审后确认无风险，执行即解冻' },
}

export const DEFECT_TYPE_MAP: Record<string, string> = {
  MOISTURE: '水分超标',
  IMPURITY: '杂质超标',
  RESIDUE: '农残/药残',
  APPEARANCE: '外观品质',
  PACKAGE: '包装破损',
  TEMPERATURE: '温湿度异常',
  OTHER: '其他',
}

export const DISPOSAL_BUSINESS_MAP: Record<string, string> = {
  GRAIN_BATCH: '原粮批次',
  PRODUCT_BATCH: '成品批次',
  MILLING_BATCH: '加工批次',
  TRACE_CODE: '单品码',
}
