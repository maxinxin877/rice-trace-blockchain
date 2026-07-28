import type { PageQuery } from './api'

export type YieldBalanceScope = 'PLANTING_TO_STORAGE' | 'STORAGE_TO_MILLING' | 'FULL_CHAIN'
export type CheckResultStatus = 'PASS' | 'WARNING' | 'FAIL'

export interface YieldBalanceCheckPayload {
  plantingBatchId?: string
  grainBatchId?: string
  productBatchId?: string
  checkScope: YieldBalanceScope
}

export interface YieldBalanceItem {
  itemName: string
  inputValue: number
  outputValue: number
  computedValue: number
  unit: string
  threshold: string
  result: CheckResultStatus
}

export interface YieldBalanceResult extends YieldBalanceCheckPayload {
  checkId: string
  result: CheckResultStatus
  items: YieldBalanceItem[]
  chainStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
  checkedAt: string
}

export interface YieldBalanceQuery extends PageQuery {
  checkId?: string
  plantingBatchId?: string
  grainBatchId?: string
  productBatchId?: string
  result?: CheckResultStatus
  startTime?: string
  endTime?: string
}

export interface RiskWarningRecord {
  warningId: string
  warningType: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  businessType: string
  businessId: string
  warningContent: string
  handled: boolean
  handledBy: string | null
  handledAt: string | null
  handleResult: string | null
  chainStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
  createdAt: string
}

export interface RiskWarningQuery extends PageQuery {
  warningType?: string
  riskLevel?: string
  businessId?: string
  handled?: boolean
}

export interface HandleRiskWarningPayload {
  handleResult: string
  reason: string
}

export interface HandleRiskWarningResult {
  warningId: string
  handled: boolean
  handledBy: string
  handledAt: string
  chainStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
}

export interface AuditLogRecord {
  auditId: string
  businessType: string
  businessId: string
  operationType: string
  operatorId: string
  operatorName: string
  beforeHash: string | null
  afterHash: string | null
  reason: string | null
  ip?: string
  operationTime: string
  txId: string | null
}

export interface AuditLogQuery extends PageQuery {
  businessType?: string
  businessId?: string
  operatorId?: string
  operationType?: string
  startTime?: string
  endTime?: string
}
