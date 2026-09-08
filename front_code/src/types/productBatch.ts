/** 成品批次实体 */
export interface RiceProductBatch {
  productBatchId: string
  tenantId: string
  productName: string
  riceVariety: string
  brandName: string
  packageSpec: string
  standardNo: string
  nutritionFacts?: NutritionFacts
  certificationFileIds?: string[]
  expectedSaleRegion?: string
  status: string
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

/** 营养成分 */
export interface NutritionFacts {
  energy?: string
  protein?: string
  fat?: string
  carbohydrate?: string
  sodium?: string
  [key: string]: string | undefined
}

/** 成品批次创建 DTO */
export interface RiceProductBatchCreateDTO {
  productBatchId: string
  productName: string
  riceVariety: string
  brandName: string
  packageSpec: string
  standardNo: string
  nutritionFacts?: NutritionFacts
  certificationFileIds?: string[]
  expectedSaleRegion?: string
}

/** 成品批次查询参数 */
export interface RiceProductBatchQuery {
  productName?: string
  brandName?: string
  riceVariety?: string
  status?: string
  page?: number
  pageSize?: number
}

/** 溯源结果（完整链路：成品 → 加工 → 入库 → 种植 → 地块 + 防伪码） */
export interface TraceResult {
  productBatch: RiceProductBatch | null
  millingBatch: Record<string, unknown> | null
  storageReceipt: Record<string, unknown> | null
  plantingBatch: Record<string, unknown> | null
  field: Record<string, unknown> | null
  traceCodes: Record<string, unknown>[]
}
