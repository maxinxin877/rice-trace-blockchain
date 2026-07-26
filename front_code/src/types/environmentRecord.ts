import type { EnvironmentSourceType } from './enums'

/** 环境数据实体 */
export interface RiceEnvironmentRecord {
  environmentRecordId: string
  tenantId: string
  plantingBatchId: string
  sourceType: EnvironmentSourceType
  recordTime: string
  airTemperature?: number
  airHumidity?: number
  soilMoisture?: number
  rainfall?: number
  windSpeed?: number
  imageFileIds?: string[]
  remark?: string
  createdBy: string
  createdAt: string
}

/** 环境数据查询参数 */
export interface RiceEnvironmentRecordQuery {
  plantingBatchId?: string
  sourceType?: EnvironmentSourceType
  recordTimeStart?: string
  recordTimeEnd?: string
  page?: number
  pageSize?: number
}
