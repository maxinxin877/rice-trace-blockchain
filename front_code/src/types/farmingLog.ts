import type { ChainStatus, FarmingOperationType } from './enums'

/** 农事记录实体 */
export interface RiceFarmingLog {
  logId: string
  tenantId: string
  plantingBatchId: string
  operationType: FarmingOperationType
  operationTime: string
  operatorId: string
  operatorName: string
  materialName?: string
  materialBatchNo?: string
  materialDosage?: number
  materialUnit?: string
  safeIntervalDays?: number
  description?: string
  proofFileIds?: string[]
  dataHash?: string
  chainStatus: ChainStatus
  txId?: string
  createdBy: string
  createdAt: string
}

/** 农事记录创建 DTO */
export interface RiceFarmingLogCreateDTO {
  plantingBatchId: string
  operationType: FarmingOperationType
  operationTime: string
  operatorId: string
  operatorName: string
  materialName?: string
  materialBatchNo?: string
  materialDosage?: number
  materialUnit?: string
  safeIntervalDays?: number
  description?: string
  proofFileIds?: string[]
}

/** 农事记录查询参数 */
export interface RiceFarmingLogQuery {
  plantingBatchId?: string
  operationType?: FarmingOperationType
  operationTimeStart?: string
  operationTimeEnd?: string
  page?: number
  pageSize?: number
}
