import type { ChainStatus } from './enums'

/** 入库单实体 */
export interface RiceStorageReceipt {
  storageReceiptId: string
  tenantId: string
  plantingBatchId: string
  grainBatchId: string
  warehouseId: string
  warehouseCode: string
  harvestAreaMu: number
  wetGrainWeightKg: number
  moisturePercent: number
  impurityPercent: number
  grainGrade: string
  storageTime: string
  temperature?: number
  humidity?: number
  keeperSignature?: string
  farmerSignature?: string
  qualityReportFileId?: string
  reportHash?: string
  dataHash?: string
  chainStatus: ChainStatus
  txId?: string
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

/** 入库单创建 DTO */
export interface RiceStorageReceiptCreateDTO {
  plantingBatchId: string
  grainBatchId: string
  warehouseId: string
  warehouseCode: string
  harvestAreaMu: number
  wetGrainWeightKg: number
  moisturePercent: number
  impurityPercent: number
  grainGrade: string
  storageTime: string
  temperature?: number
  humidity?: number
  keeperSignature?: string
  farmerSignature?: string
  qualityReportFileId?: string
}

/** 入库单查询参数 */
export interface RiceStorageReceiptQuery {
  plantingBatchId?: string
  grainBatchId?: string
  warehouseCode?: string
  storageTimeStart?: string
  storageTimeEnd?: string
  chainStatus?: ChainStatus
  page?: number
  pageSize?: number
}
