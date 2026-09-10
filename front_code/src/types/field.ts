import type { ChainStatus } from './enums'

/** 地块实体 */
export interface RiceField {
  fieldId: string
  tenantId: string
  fieldCode: string
  fieldName: string
  farmerId: string
  farmerName: string
  province: string
  city: string
  district: string
  address: string
  areaMu: number
  gisBoundary: GisPoint[]
  soilType?: string
  basePhotoFileIds?: string[]
  coordinateHash?: string
  chainStatus: ChainStatus
  txId?: string
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

/** GIS 坐标点 */
export interface GisPoint {
  lat: number
  lng: number
}

/** 地块创建 DTO */
export interface RiceFieldCreateDTO {
  fieldCode: string
  fieldName: string
  farmerId: string
  farmerName: string
  province: string
  city: string
  district: string
  address: string
  areaMu: number
  gisBoundary: GisPoint[]
  soilType?: string
  basePhotoFileIds?: string[]
}

/** 地块更新 DTO */
export interface RiceFieldUpdateDTO extends Partial<RiceFieldCreateDTO> {
  fieldId: string
  reason?: string
}

/** 地块查询参数 */
export interface RiceFieldQuery {
  fieldCode?: string
  fieldName?: string
  farmerName?: string
  province?: string
  city?: string
  chainStatus?: ChainStatus
  page?: number
  pageSize?: number
}
