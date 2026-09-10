import type { ChainStatus, QualityBusinessType, QualityResult } from './enums'

/** 质检主表实体 */
export interface RiceQualityTest {
  qualityTestId: string
  tenantId: string
  businessType: QualityBusinessType
  businessId: string
  testAgency: string
  testTime: string
  overallResult: QualityResult
  reportFileId?: string
  reportHash?: string
  remark?: string
  dataHash?: string
  chainStatus: ChainStatus
  txId?: string
  createdBy: string
  createdAt: string
  items?: RiceQualityTestItem[]
}

/** 质检项目实体 */
export interface RiceQualityTestItem {
  itemId: string
  qualityTestId: string
  itemName: string
  itemValue: number
  unit?: string
  standardValue?: string
  result: QualityResult
}

/** 质检创建 DTO */
export interface RiceQualityTestCreateDTO {
  businessType: QualityBusinessType
  businessId: string
  testAgency: string
  testTime: string
  overallResult: QualityResult
  reportFileId?: string
  remark?: string
  items: RiceQualityTestItemCreateDTO[]
}

/** 质检项目创建 DTO */
export interface RiceQualityTestItemCreateDTO {
  itemName: string
  itemValue: number
  unit?: string
  standardValue?: string
  result: QualityResult
}
