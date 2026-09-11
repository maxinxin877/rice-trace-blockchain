import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  ActivateTraceCodePayload,
  ActivateTraceCodeResult,
  CertificateRecord,
  ChainProofRecord,
  ChannelWarningRecord,
  GenerateTraceCodePayload,
  GenerateTraceCodeResult,
  TraceCodeQuery,
  TraceCodeRecord,
  TraceDetail,
  TraceNode,
  VerifyResult,
} from '@/types/traceability'
import { mockDelay, mockPaginate } from '@/api/mock'
import { chainProofs, channelWarnings, scanLogs, traceCodes, traceDetails } from '@/api/mock/data/traceability'
import { mockProductBatches } from '@/api/mock/data/productBatches'
import { get, post } from '@/api/request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

function ok<T>(data: T, message = 'success'): ApiResponse<T> {
  return { code: 200, message, data }
}

function mockNow(): string {
  return '2026-07-26 11:30:00'
}

function pageParams(query: TraceCodeQuery) {
  const { page, ...rest } = query
  return { ...rest, pageNo: page || 1, pageSize: query.pageSize || 10 }
}

/* ============ 小程序真实接口（/mini/rice/trace/**）数据适配 ============ */

interface MiniTraceProductRaw {
  productBatchId?: string
  productName?: string
  brandName?: string
  packageSpec?: string
  standardNo?: string
  riceVariety?: string
  expectedSaleRegion?: string
  nutritionFacts?: Record<string, unknown> | null
  status?: string
}

interface MiniTracePlantingRaw {
  fieldName?: string
  address?: string
  areaMu?: number
  basePhotos?: string[]
  plantingBatchId?: string
  riceVariety?: string
  sowingDate?: string
  actualHarvestDate?: string
  organicCertified?: boolean
  greenCertified?: boolean
}

interface MiniFarmingLogRaw {
  operationType?: string
  operationTime?: string
  operatorName?: string
  materialName?: string
  materialDosage?: number | string
  materialUnit?: string
  safeIntervalDays?: number
  description?: string
  dataHash?: string
  chainStatus?: string
}

interface MiniStorageRaw {
  storageReceiptId?: string
  grainGrade?: string
  storageTime?: string
  moisturePercent?: number
  impurityPercent?: number
  temperature?: number
  humidity?: number
  warehouseCode?: string
  chainStatus?: string
}

interface MiniMillingRaw {
  millingBatchId?: string
  processFlow?: string[]
  yieldRate?: number
  qualitySummary?: string
  processStartTime?: string
  processEndTime?: string
  chainStatus?: string
  qualityResult?: string
  testAgency?: string
}

interface MiniTraceDetailRaw {
  traceCode?: string
  product?: MiniTraceProductRaw | null
  authenticity?: { status?: string; scanCount?: number; displayText?: string; riskLevel?: string } | null
  planting?: MiniTracePlantingRaw | null
  farmingLogs?: MiniFarmingLogRaw[] | null
  storage?: MiniStorageRaw | null
  milling?: MiniMillingRaw | null
  certifications?: { fileId?: string; name?: string; fileUrl?: string }[] | null
  chainProof?: { verified?: boolean; dataHash?: string; txId?: string; blockHeight?: number; chainTime?: string } | null
}

interface MiniVerifyRaw {
  traceCode?: string
  authentic?: boolean
  firstScan?: boolean
  scanCount?: number
  displayText?: string
  riskLevel?: string
  channelWarning?: boolean
  chainVerified?: boolean
  firstScannedAt?: string | null
  currentScanAt?: string | null
  codeStatus?: string
}

interface MiniChainProofRaw {
  traceCode?: string
  verified?: boolean
  productBatchId?: string
  dataHash?: string
  chainHash?: string
  txId?: string
  blockHeight?: number
  chainTime?: string
}

const FARMING_OPERATION_LABEL: Record<string, string> = {
  SOWING: '播种',
  FERTILIZING: '施肥',
  FERTILIZATION: '施肥',
  PESTICIDE: '用药',
  IRRIGATION: '灌溉',
  WEEDING: '除草',
  HARVEST: '收割',
}

function chainStatusOf(value?: string): 'PENDING' | 'SUCCESS' | 'FAILED' {
  return value === 'SUCCESS' || value === 'FAILED' ? value : 'PENDING'
}

/** 后端小程序溯源详情 -> 前端 TraceDetail */
function mapTraceDetail(raw: MiniTraceDetailRaw | null): TraceDetail | null {
  if (!raw) return null
  const product: MiniTraceProductRaw = raw.product || {}
  const planting: MiniTracePlantingRaw = raw.planting || {}
  const storage: MiniStorageRaw = raw.storage || {}
  const milling: MiniMillingRaw = raw.milling || {}
  const authenticity: NonNullable<MiniTraceDetailRaw['authenticity']> = raw.authenticity || {}

  const nutritionFacts: Record<string, string> = {}
  Object.entries(product.nutritionFacts || {}).forEach(([key, value]) => {
    nutritionFacts[key] = value == null ? '' : String(value)
  })

  const timeline: TraceNode[] = []
  if (planting.sowingDate || planting.plantingBatchId) {
    timeline.push({
      stage: 'PLANTING',
      title: '播种建批次',
      time: planting.sowingDate || '',
      organization: planting.fieldName || '',
      location: planting.address || '',
      summary: `${planting.riceVariety || product.riceVariety || ''} 完成播种，进入种植管理`,
      businessId: planting.plantingBatchId || '',
      chainStatus: raw.chainProof?.verified ? 'SUCCESS' : 'PENDING',
    })
  }
  ;(raw.farmingLogs || []).forEach((log: MiniFarmingLogRaw, index: number) => {
    timeline.push({
      stage: 'FARMING',
      title: FARMING_OPERATION_LABEL[log.operationType || ''] || log.operationType || '农事操作',
      time: log.operationTime || '',
      organization: log.operatorName || '',
      location: planting.address || '',
      summary: [
        log.materialName,
        log.materialDosage != null ? `${log.materialDosage}${log.materialUnit || ''}` : '',
        log.description || '',
      ].filter(Boolean).join(' · '),
      businessId: `${planting.plantingBatchId || 'FARM'}-${index}`,
      chainStatus: chainStatusOf(log.chainStatus),
    })
  })
  if (storage.storageReceiptId) {
    timeline.push({
      stage: 'STORAGE',
      title: '收储入库',
      time: storage.storageTime || '',
      organization: storage.warehouseCode || '',
      location: planting.address || '',
      summary: `原粮等级 ${storage.grainGrade || '-'} · 水分 ${storage.moisturePercent ?? '-'}% · 仓储 ${storage.temperature ?? '-'}℃/${storage.humidity ?? '-'}%`,
      businessId: storage.storageReceiptId,
      chainStatus: chainStatusOf(storage.chainStatus),
    })
  }
  if (milling.millingBatchId) {
    timeline.push({
      stage: 'MILLING',
      title: '碾米加工',
      time: milling.processStartTime || '',
      organization: '加工厂',
      location: planting.address || '',
      summary: `${(milling.processFlow || []).join(' → ') || '加工'} · 产出率 ${milling.yieldRate ?? '-'}% · ${milling.qualitySummary || ''}`,
      businessId: milling.millingBatchId,
      chainStatus: chainStatusOf(milling.chainStatus),
    })
  }
  if (product.productBatchId) {
    timeline.push({
      stage: 'PRODUCT',
      title: '包装成品',
      time: milling.processEndTime || '',
      organization: product.brandName || '',
      location: '',
      summary: `${product.productName || ''} · ${product.packageSpec || ''} · 执行标准 ${product.standardNo || '-'}`,
      businessId: product.productBatchId,
      chainStatus: 'SUCCESS',
    })
  }

  const certificates: CertificateRecord[] = (raw.certifications || []).map((item: { fileId?: string; name?: string; fileUrl?: string }, index: number) => ({
    certificateId: item.fileId || `CERT-${index + 1}`,
    name: item.name || '认证证书',
    authority: '',
    certificateNo: '',
    validFrom: '',
    validTo: '',
    status: 'VALID',
    fileType: 'PDF',
  }))

  return {
    traceCode: {
      traceCode: raw.traceCode || '',
      productBatchId: product.productBatchId || '',
      productName: product.productName || '',
      brandName: product.brandName || '',
      riceVariety: product.riceVariety || '',
      packageSpec: product.packageSpec || '',
      expectedSaleRegion: product.expectedSaleRegion || '',
      status: (authenticity.status as TraceCodeRecord['status']) || 'GENERATED',
      scanCount: authenticity.scanCount || 0,
      firstScannedAt: null,
      lastScannedAt: null,
      riskLevel: (authenticity.riskLevel as TraceCodeRecord['riskLevel']) || 'LOW',
      chainStatus: raw.chainProof && raw.chainProof.verified ? 'SUCCESS' : 'PENDING',
      createdAt: '',
    },
    fieldName: planting.fieldName || '',
    origin: planting.address || '',
    harvestDate: planting.actualHarvestDate || '',
    productionDate: milling.processStartTime || storage.storageTime || '',
    qualityResult: milling.qualityResult || milling.qualitySummary || '待检',
    nutritionFacts,
    certificates,
    timeline,
  }
}

/** 后端真伪鉴别结果 -> 前端 VerifyResult */
function mapVerifyResult(raw: MiniVerifyRaw | null): VerifyResult {
  if (!raw) {
    return {
      traceCode: '',
      verified: false,
      result: 'INVALID',
      title: '未查询到有效防伪码',
      message: '请核对包装上的防伪码，谨防假冒产品。',
      scanCount: 0,
      firstScannedAt: null,
      currentScanAt: '',
      riskTips: ['不要购买来源不明或包装破损的产品'],
    }
  }
  const authentic = raw.authentic === true
  const firstScan = raw.firstScan === true
  const riskLevel = raw.riskLevel || 'LOW'
  const result: VerifyResult['result'] = authentic
    ? (firstScan ? 'GENUINE' : 'REPEAT')
    : (raw.codeStatus === 'INVALID' ? 'INVALID' : 'RISK')
  const riskTips: string[] = []
  if (!authentic) riskTips.push('未激活、已停用或已过期的防伪码不作为正品凭证')
  if (raw.channelWarning) riskTips.push('该码在预期销售区域外被扫码，请核对购买渠道')
  if (!authentic || riskLevel !== 'LOW') riskTips.push('如包装异常，请联系品牌客服或监管部门')
  return {
    traceCode: raw.traceCode || '',
    verified: authentic,
    result,
    title: authentic ? (firstScan ? '正品 · 首次查询' : '正品 · 非首次查询') : '该防伪码存在风险',
    message: raw.displayText || (authentic ? '该防伪码已通过系统校验' : '请核对包装上的防伪码，谨防假冒产品'),
    scanCount: raw.scanCount || 0,
    firstScannedAt: raw.firstScannedAt || null,
    currentScanAt: raw.currentScanAt || '',
    riskTips,
  }
}

/** 后端链上核验结果 -> 前端 ChainProofRecord */
function mapMiniChainProof(raw: MiniChainProofRaw | null): ChainProofRecord | null {
  if (!raw) return null
  return {
    businessType: 'TRACE_CODE',
    businessId: raw.productBatchId || raw.traceCode || '',
    dataHash: raw.dataHash || '',
    fileHashes: [],
    txId: raw.txId || '',
    blockHeight: raw.blockHeight || 0,
    chainTime: raw.chainTime || '',
    chainStatus: raw.verified ? 'SUCCESS' : 'PENDING',
    verified: raw.verified === true,
    currentHash: raw.dataHash || '',
    chainHash: raw.chainHash || '',
  }
}
interface RiceChainProofRaw {
  chainProofId?: string
  businessType?: string
  businessId?: string
  dataHash?: string
  fileHashes?: string[] | null
  txId?: string
  blockHeight?: number
  chainTime?: string
  chainStatus?: string
  chainError?: string
}

/** 后端链上存证记录 -> 前端 ChainProofRecord */
function mapChainProofRecord(raw: RiceChainProofRaw): ChainProofRecord {
  const success = raw.chainStatus === 'SUCCESS'
  return {
    businessType: raw.businessType || '',
    businessId: raw.businessId || '',
    dataHash: raw.dataHash || '',
    fileHashes: raw.fileHashes || [],
    txId: raw.txId || '',
    blockHeight: raw.blockHeight || 0,
    chainTime: raw.chainTime || '',
    chainStatus: chainStatusOf(raw.chainStatus),
    verified: success,
    currentHash: raw.dataHash || '',
    chainHash: raw.dataHash || '',
  }
}
export const traceabilityApi = {
  async getTraceCodes(query: TraceCodeQuery = {}): Promise<PageResponse<TraceCodeRecord>> {
    if (!USE_MOCK) return (await get<PageResponse<TraceCodeRecord>['data']>('/rice/trace-codes', pageParams(query))).data
    await mockDelay()
    let list = [...traceCodes]
    if (query.traceCode) list = list.filter((item) => item.traceCode.includes(query.traceCode!))
    if (query.productBatchId) list = list.filter((item) => item.productBatchId.includes(query.productBatchId!))
    if (query.status) list = list.filter((item) => item.status === query.status)
    if (query.riskLevel) list = list.filter((item) => item.riskLevel === query.riskLevel)
    return ok(mockPaginate(list, query.page || 1, query.pageSize || 10))
  },

  async generateTraceCodes(payload: GenerateTraceCodePayload): Promise<ApiResponse<GenerateTraceCodeResult>> {
    if (!USE_MOCK) return (await post<GenerateTraceCodeResult>('/rice/trace-codes/generate', payload)).data
    await mockDelay(500)
    const product = mockProductBatches.find((item) => item.productBatchId === payload.productBatchId)
    if (!product) return { code: 404, message: '成品批次不存在', data: null as unknown as GenerateTraceCodeResult }
    const taskId = `TCG20260726${String(traceCodes.length + 1).padStart(4, '0')}`
    const seed = String(traceCodes.length + 1).padStart(5, '0')
    const created = Array.from({ length: payload.quantity }, (_, index): TraceCodeRecord => ({
      traceCode: `RC20260726${seed}${String(index + 1).padStart(3, '0')}`,
      productBatchId: product.productBatchId,
      productName: product.productName,
      brandName: product.brandName,
      riceVariety: product.riceVariety,
      packageSpec: payload.packageSpec,
      expectedSaleRegion: payload.expectedSaleRegion,
      status: 'GENERATED',
      scanCount: 0,
      firstScannedAt: null,
      lastScannedAt: null,
      riskLevel: 'LOW',
      chainStatus: 'PENDING',
      createdAt: mockNow(),
    }))
    traceCodes.unshift(...created)
    return ok({ generateTaskId: taskId, productBatchId: payload.productBatchId, quantity: payload.quantity, status: 'COMPLETED' }, '生成任务已完成')
  },

  async activateTraceCodes(payload: ActivateTraceCodePayload): Promise<ApiResponse<ActivateTraceCodeResult>> {
    if (!USE_MOCK) return (await post<ActivateTraceCodeResult>('/rice/trace-codes/activate', payload)).data
    await mockDelay(350)
    let activatedCount = 0
    traceCodes.forEach((item) => {
      if (payload.traceCodes.includes(item.traceCode) && item.productBatchId === payload.productBatchId && item.status === 'GENERATED') {
        item.status = 'ACTIVATED'
        item.chainStatus = 'PENDING'
        activatedCount += 1
      }
    })
    return ok({ activatedCount, failedCount: payload.traceCodes.length - activatedCount, chainStatus: 'PENDING' }, '激活完成')
  },

  async getChannelWarnings(): Promise<ApiResponse<ChannelWarningRecord[]>> {
    if (!USE_MOCK) {
      const response = (await get<PageResponse<ChannelWarningRecord>['data']>('/rice/channel-warnings', { pageNo: 1, pageSize: 100 })).data
      return ok(response.data.records)
    }
    await mockDelay()
    return ok([...channelWarnings])
  },

  async getTraceDetail(traceCode: string): Promise<ApiResponse<TraceDetail | null>> {
    if (!USE_MOCK) {
      const res = await get<MiniTraceDetailRaw>(`/mini/rice/trace/${traceCode}`)
      return ok(mapTraceDetail(res.data.data))
    }
    await mockDelay(350)
    return ok(traceDetails[traceCode] || null)
  },

  async verifyTraceCode(traceCode: string, region = '未知地区'): Promise<ApiResponse<VerifyResult>> {
    if (!USE_MOCK) {
      const res = await post<MiniVerifyRaw>(`/mini/rice/trace/${traceCode}/verify`, { region })
      return ok(mapVerifyResult(res.data.data))
    }
    await mockDelay(450)
    const record = traceCodes.find((item) => item.traceCode === traceCode)
    const currentScanAt = mockNow()
    if (!record || record.status === 'DEACTIVATED') return ok({ traceCode, verified: false, result: 'INVALID', title: '未查询到有效防伪码', message: '请核对包装上的防伪码，谨防假冒产品。', scanCount: record?.scanCount || 0, firstScannedAt: record?.firstScannedAt || null, currentScanAt, riskTips: ['不要购买来源不明或包装破损的产品'] })
    if (record.status === 'GENERATED') return ok({ traceCode, verified: false, result: 'RISK', title: '防伪码尚未激活', message: '该产品尚未完成出库激活，请联系销售方核实。', scanCount: record.scanCount, firstScannedAt: record.firstScannedAt, currentScanAt, riskTips: ['未激活防伪码不作为正品凭证'] })
    record.scanCount += 1
    record.lastScannedAt = currentScanAt
    const firstScan = !record.firstScannedAt
    if (firstScan) record.firstScannedAt = currentScanAt
    const risky = record.status === 'RISK' || record.riskLevel === 'HIGH'
    const result = risky ? 'RISK' : firstScan ? 'GENUINE' : 'REPEAT'
    scanLogs.unshift({ scanId: `SCAN20260726${String(scanLogs.length + 1).padStart(4, '0')}`, traceCode, scanTime: currentScanAt, province: region, city: region, region, firstScan, result, scene: '消费者扫码', ip: '客户端隐私保护' })
    return ok({ traceCode, verified: !risky, result, title: risky ? '该防伪码存在风险' : firstScan ? '正品 · 首次查询' : '正品 · 非首次查询', message: risky ? '系统检测到高频或跨区域扫码，请核对购买渠道。' : firstScan ? '该防伪码已通过系统校验，且为首次扫码。' : `该产品已被查询 ${record.scanCount} 次，请结合首次查询时间判断。`, scanCount: record.scanCount, firstScannedAt: record.firstScannedAt, currentScanAt, riskTips: risky ? ['核对销售渠道与发票', '如包装异常，请联系品牌客服或监管部门'] : [] })
  },

  /** 小程序链上核验（公开接口） */
  async getMiniChainProof(traceCode: string): Promise<ApiResponse<ChainProofRecord | null>> {
    if (!USE_MOCK) {
      const res = await get<MiniChainProofRaw>(`/mini/rice/trace/${traceCode}/chain-proof`)
      return ok(mapMiniChainProof(res.data.data))
    }
    await mockDelay(300)
    return ok(chainProofs.find((item) => item.businessId === traceCode) || chainProofs[0] || null)
  },

  /** 小程序重新核验链上存证 */
  async reVerifyMiniChainProof(traceCode: string): Promise<ApiResponse<ChainProofRecord | null>> {
    if (!USE_MOCK) {
      const res = await post<MiniChainProofRaw>(`/mini/rice/trace/${traceCode}/chain-proof`)
      return ok(mapMiniChainProof(res.data.data))
    }
    await mockDelay(600)
    const proof = chainProofs.find((item) => item.businessId === traceCode) || chainProofs[0] || null
    if (proof) proof.verified = proof.currentHash === proof.chainHash
    return ok(proof)
  },
  async getChainProof(businessType: string, businessId: string): Promise<ApiResponse<ChainProofRecord | null>> {
    if (!USE_MOCK) return (await get<ChainProofRecord>(`/rice/chain-proofs/${businessType}/${businessId}`)).data
    await mockDelay(300)
    return ok(chainProofs.find((item) => item.businessType === businessType && item.businessId === businessId) || null)
  },

  async verifyChainProof(businessType: string, businessId: string): Promise<ApiResponse<ChainProofRecord | null>> {
    if (!USE_MOCK) return (await post<ChainProofRecord>(`/rice/chain-proofs/${businessType}/${businessId}/verify`)).data
    await mockDelay(600)
    const proof = chainProofs.find((item) => item.businessType === businessType && item.businessId === businessId) || null
    if (proof) proof.verified = proof.currentHash === proof.chainHash
    return ok(proof)
  },

  async getChainProofs(): Promise<ApiResponse<ChainProofRecord[]>> {
    if (!USE_MOCK) {
      const res = await get<PageResponse<RiceChainProofRaw>['data']>('/rice/chain-proofs', { pageNo: 1, pageSize: 100 })
      return ok((res.data.data.records || []).map(mapChainProofRecord))
    }
    await mockDelay()
    return ok([...chainProofs])
  },
}
