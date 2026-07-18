import type { ChainStatus, RiceBatchStatus } from './enums'

/** 种植批次实体 */
export interface RicePlantingBatch {
  plantingBatchId: string
  tenantId: string
  fieldId: string
  fieldCode?: string
  fieldName?: string
  riceVariety: string
  seedSource: string
  seedBatchNo?: string
  seedCertificateFileId?: string
  sowingDate: string
  expectedHarvestDate?: string
  actualHarvestDate?: string
  status: RiceBatchStatus
  organicCertified: boolean
  greenCertified: boolean
  certificationFileIds?: string[]
  dataHash?: string
  chainStatus: ChainStatus
  txId?: string
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

/** 种植批次创建 DTO */
export interface RicePlantingBatchCreateDTO {
  fieldId: string
  riceVariety: string
  seedSource: string
  seedBatchNo?: string
  seedCertificateFileId?: string
  sowingDate: string
  expectedHarvestDate?: string
  organicCertified: boolean
  greenCertified: boolean
  certificationFileIds?: string[]
}

/** 种植批次查询参数 */
export interface RicePlantingBatchQuery {
  fieldId?: string
  riceVariety?: string
  status?: RiceBatchStatus
  sowingDateStart?: string
  sowingDateEnd?: string
  page?: number
  pageSize?: number
}
