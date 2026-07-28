import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  ActivateTraceCodePayload,
  ActivateTraceCodeResult,
  ChainProofRecord,
  ChannelWarningRecord,
  GenerateTraceCodePayload,
  GenerateTraceCodeResult,
  ScanLogRecord,
  TraceCodeQuery,
  TraceCodeRecord,
  TraceDetail,
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

  async getScanLogs(traceCode: string): Promise<ApiResponse<ScanLogRecord[]>> {
    if (!USE_MOCK) return (await get<ScanLogRecord[]>(`/rice/trace-codes/${traceCode}/scan-logs`)).data
    await mockDelay(200)
    return ok(scanLogs.filter((item) => item.traceCode === traceCode))
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
    await mockDelay(350)
    return ok(traceDetails[traceCode] || null)
  },

  async verifyTraceCode(traceCode: string, region = '未知地区'): Promise<ApiResponse<VerifyResult>> {
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
    await mockDelay()
    return ok([...chainProofs])
  },
}
