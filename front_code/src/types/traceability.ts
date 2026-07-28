export interface TraceCodeRecord {
  traceCode: string
  productBatchId: string
  productName: string
  brandName: string
  riceVariety: string
  packageSpec: string
  expectedSaleRegion: string
  status: 'GENERATED' | 'ACTIVATED' | 'DEACTIVATED' | 'RISK'
  scanCount: number
  firstScannedAt: string | null
  lastScannedAt: string | null
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  chainStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
  createdAt: string
}

export interface ScanLogRecord {
  scanId: string
  traceCode: string
  scanTime: string
  province: string
  city: string
  region: string
  firstScan: boolean
  result: 'GENUINE' | 'REPEAT' | 'RISK' | 'INVALID'
  scene: string
  ip: string
}

export interface CertificateRecord {
  certificateId: string
  name: string
  authority: string
  certificateNo: string
  validFrom: string
  validTo: string
  status: 'VALID' | 'EXPIRING' | 'EXPIRED'
  fileType: 'PDF' | 'IMAGE'
}

export interface TraceNode {
  stage: string
  title: string
  time: string
  organization: string
  location: string
  summary: string
  businessId: string
  chainStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
}

export interface TraceDetail {
  traceCode: TraceCodeRecord
  fieldName: string
  origin: string
  harvestDate: string
  productionDate: string
  qualityResult: string
  nutritionFacts: Record<string, string>
  certificates: CertificateRecord[]
  timeline: TraceNode[]
}

export interface VerifyResult {
  traceCode: string
  verified: boolean
  result: 'GENUINE' | 'REPEAT' | 'RISK' | 'INVALID'
  title: string
  message: string
  scanCount: number
  firstScannedAt: string | null
  currentScanAt: string
  riskTips: string[]
}

export interface ChainProofRecord {
  businessType: string
  businessId: string
  dataHash: string
  fileHashes: string[]
  txId: string
  blockHeight: number
  chainTime: string
  chainStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
  verified: boolean
  currentHash: string
  chainHash: string
}

export interface ChannelWarningRecord {
  warningId: string
  traceCode: string
  productName: string
  expectedRegion: string
  actualRegion: string
  scanCount: number
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  warningTime: string
  handled: boolean
}

export interface TraceCodeQuery {
  traceCode?: string
  productBatchId?: string
  status?: string
  riskLevel?: string
  page?: number
  pageSize?: number
}

export interface GenerateTraceCodePayload {
  productBatchId: string
  quantity: number
  packageSpec: string
  expectedSaleRegion: string
  expireDays?: number
}

export interface GenerateTraceCodeResult {
  generateTaskId: string
  productBatchId: string
  quantity: number
  status: 'PROCESSING' | 'COMPLETED'
}

export interface ActivateTraceCodePayload {
  productBatchId: string
  traceCodes: string[]
  activatedBy: string
  activatedAt?: string
  reason?: string
}

export interface ActivateTraceCodeResult {
  activatedCount: number
  failedCount: number
  chainStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
}
