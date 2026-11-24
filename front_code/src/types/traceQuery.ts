/** 追溯查询类型（对应后端 /rice/trace/**） */

export type TraceBusinessType =
  | 'TRACE_CODE'
  | 'BOX_CODE'
  | 'PALLET_CODE'
  | 'PRODUCT_BATCH'
  | 'MILLING_BATCH'
  | 'GRAIN_BATCH'
  | 'STORAGE_RECEIPT'
  | 'PLANTING_BATCH'
  | 'FIELD'

export interface TraceResolveResult {
  keyword: string
  matchedType: TraceBusinessType
  matchedId: string
  label: string
  note: string
}

export interface TraceChainNode {
  nodeKey: string
  nodeType: string
  stage: string
  title: string
  businessType: string
  businessId: string
  time: string
  organization: string
  location: string
  summary: string
  status?: string
  chainStatus?: string
  txId?: string
  dataHash?: string
  metrics?: Record<string, unknown>
  fileIds?: string[]
  risky?: boolean
  riskNote?: string
  detailRoute?: string
}

export interface TraceChainEdge {
  from: string
  to: string
  relation: string
  label: string
  quantity: string
}

export interface TraceTimelineItem {
  time: string
  stage: string
  title: string
  description: string
  businessType: string
  businessId: string
  organization?: string
  chainStatus?: string
  txId?: string
  fileIds?: string[]
  risky?: boolean
}

export interface TraceChainProof {
  stage: string
  businessType: string
  businessId: string
  dataHash: string
  txId: string
  blockHeight: number
  chainTime: string
  chainStatus: string
  fileHashes?: string[]
}

export interface TraceChain {
  rootType: string
  rootId: string
  rootLabel: string
  direction: string
  nodes: TraceChainNode[]
  edges: TraceChainEdge[]
  timeline: TraceTimelineItem[]
  summary: Record<string, number | string>
  chainProofs: TraceChainProof[]
  tips: string[]
}

export interface TraceSearchResult {
  resolved: TraceResolveResult
  chain: TraceChain
}

export interface TraceImpactProduct {
  productBatchId: string
  productName: string
  brandName: string
  packageSpec: string
  status: string
  expectedSaleRegion: string
  traceCodeCount: number
}

export interface TraceImpactRegion {
  region: string
  count: number
  source: 'SCAN' | 'EXPECTED'
}

export interface TraceImpact {
  rootType: string
  rootId: string
  rootLabel: string
  grainBatches: string[]
  millingBatches: string[]
  productBatches: TraceImpactProduct[]
  traceCodeCount: number
  traceCodeSamples: string[]
  quantity: number
  weightKg: number
  regions: TraceImpactRegion[]
  scannedCodeCount: number
  frozenCodeCount: number
  tips: string[]
}

/** 节点环节排序（链路图横向排列顺序） */
export const TRACE_STAGE_ORDER = ['种植', '收储', '加工', '包装', '流通'] as const

/** 节点类型 → 主题色 */
export const TRACE_NODE_COLOR: Record<string, string> = {
  FIELD: '#2f7a4d',
  PLANTING_BATCH: '#409eff',
  STORAGE_RECEIPT: '#65a765',
  MILLING_BATCH: '#e7b84b',
  PRODUCT_BATCH: '#d97706',
  TRACE_CODE: '#7a5af8',
}
