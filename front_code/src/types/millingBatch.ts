import type { ChainStatus } from './enums'

/** 加工批次实体 */
export interface RiceMillingBatch {
  millingBatchId: string
  tenantId: string
  grainBatchId: string
  productBatchId: string
  factoryId: string
  grainOutWeightKg: number
  riceOutputWeightKg?: number
  yieldRate?: number
  processStartTime: string
  processEndTime?: string
  processParams: MillingProcessParams
  qualitySummary?: string
  qualityReportFileId?: string
  dataHash?: string
  chainStatus: ChainStatus
  txId?: string
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

/** 加工工艺参数 */
export interface MillingProcessParams {
  husking?: string    // 脱壳
  polishing?: string  // 抛光
  colorSorting?: string // 色选
  packaging?: string  // 包装
  [key: string]: string | undefined
}

/** 加工批次创建 DTO */
export interface RiceMillingBatchCreateDTO {
  grainBatchId: string
  productBatchId: string
  factoryId: string
  grainOutWeightKg: number
  processStartTime: string
  processParams: MillingProcessParams
  qualityReportFileId?: string
}

/** 完成加工 DTO */
export interface RiceMillingCompleteDTO {
  millingBatchId: string
  riceOutputWeightKg: number
  processEndTime: string
  qualitySummary?: string
  processParams?: MillingProcessParams
}

/** 加工批次查询参数 */
export interface RiceMillingBatchQuery {
  grainBatchId?: string
  productBatchId?: string
  processStartTimeStart?: string
  processStartTimeEnd?: string
  chainStatus?: ChainStatus
  page?: number
  pageSize?: number
}
