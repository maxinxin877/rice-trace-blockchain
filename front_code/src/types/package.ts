/** 箱码/托盘码类型（对应后端 /rice/packages/**） */

export interface PackageUnit {
  unitCode: string
  unitLevel: 'BOX' | 'PALLET'
  parentCode: string
  productBatchId: string
  packageSpec: string
  childLevel: 'TRACE_CODE' | 'BOX'
  childCount: number
  totalQuantity: number
  status: string
  expectedSaleRegion: string
  createTime?: string
}

export interface PackageChild {
  childCode: string
  childLevel: 'TRACE_CODE' | 'BOX'
  status?: string
  scanCount?: number
  packageSpec?: string
  childCount?: number
  totalQuantity?: number
}

export interface PackageDetail {
  unit: PackageUnit
  children: PackageChild[]
  traceCodes: string[]
  parent?: PackageUnit | null
}

export interface PackageCreatePayload {
  unitLevel: 'BOX' | 'PALLET'
  productBatchId?: string
  childCodes: string[]
  packageSpec?: string
  expectedSaleRegion?: string
}

export const UNIT_LEVEL_MAP: Record<string, string> = {
  BOX: '箱码',
  PALLET: '托盘码',
}
