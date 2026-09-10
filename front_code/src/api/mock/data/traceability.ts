import type { CertificateRecord, ChainProofRecord, ChannelWarningRecord, ScanLogRecord, TraceCodeRecord, TraceDetail } from '@/types/traceability'

export const traceCodes: TraceCodeRecord[] = [
  { traceCode: 'RC20260718000001', productBatchId: 'PROD20260701001', productName: '五常有机稻花香大米', brandName: '稻香源', riceVariety: '稻花香2号', packageSpec: '5kg/袋', expectedSaleRegion: '北京、天津、河北', status: 'ACTIVATED', scanCount: 6, firstScannedAt: '2026-07-20 10:22:16', lastScannedAt: '2026-07-26 09:18:35', riskLevel: 'LOW', chainStatus: 'SUCCESS', createdAt: '2026-07-18 09:10:00' },
  { traceCode: 'RC20260718000002', productBatchId: 'PROD20260701001', productName: '五常有机稻花香大米', brandName: '稻香源', riceVariety: '稻花香2号', packageSpec: '5kg/袋', expectedSaleRegion: '北京、天津、河北', status: 'RISK', scanCount: 38, firstScannedAt: '2026-07-21 08:31:09', lastScannedAt: '2026-07-26 08:52:11', riskLevel: 'HIGH', chainStatus: 'SUCCESS', createdAt: '2026-07-18 09:10:00' },
  { traceCode: 'RC20260719000003', productBatchId: 'PROD20260701004', productName: '盘锦蟹田大米', brandName: '盘锦稻田', riceVariety: '辽粳9号', packageSpec: '5kg/袋', expectedSaleRegion: '辽宁、北京', status: 'ACTIVATED', scanCount: 2, firstScannedAt: '2026-07-24 16:10:20', lastScannedAt: '2026-07-25 11:05:03', riskLevel: 'LOW', chainStatus: 'SUCCESS', createdAt: '2026-07-19 14:20:00' },
  { traceCode: 'RC20260722000004', productBatchId: 'PROD20260701002', productName: '五常大米（优质）', brandName: '五常御品', riceVariety: '五优稻4号', packageSpec: '10kg/袋', expectedSaleRegion: '东北三省', status: 'GENERATED', scanCount: 0, firstScannedAt: null, lastScannedAt: null, riskLevel: 'LOW', chainStatus: 'PENDING', createdAt: '2026-07-22 15:30:00' },
  { traceCode: 'RC20260723000005', productBatchId: 'PROD20260701004', productName: '盘锦蟹田大米', brandName: '盘锦稻田', riceVariety: '辽粳9号', packageSpec: '5kg/袋', expectedSaleRegion: '辽宁、北京', status: 'DEACTIVATED', scanCount: 1, firstScannedAt: '2026-07-24 13:28:00', lastScannedAt: '2026-07-24 13:28:00', riskLevel: 'MEDIUM', chainStatus: 'FAILED', createdAt: '2026-07-23 08:45:00' },
]

export const scanLogs: ScanLogRecord[] = [
  { scanId: 'SCAN202607200001', traceCode: 'RC20260718000001', scanTime: '2026-07-20 10:22:16', province: '北京市', city: '北京市', region: '朝阳区', firstScan: true, result: 'GENUINE', scene: '消费者扫码', ip: '120.245.*.*' },
  { scanId: 'SCAN202607260002', traceCode: 'RC20260718000001', scanTime: '2026-07-26 09:18:35', province: '北京市', city: '北京市', region: '海淀区', firstScan: false, result: 'REPEAT', scene: '消费者扫码', ip: '120.245.*.*' },
  { scanId: 'SCAN202607210003', traceCode: 'RC20260718000002', scanTime: '2026-07-21 08:31:09', province: '北京市', city: '北京市', region: '丰台区', firstScan: true, result: 'GENUINE', scene: '消费者扫码', ip: '111.192.*.*' },
  { scanId: 'SCAN202607260004', traceCode: 'RC20260718000002', scanTime: '2026-07-26 08:52:11', province: '广东省', city: '深圳市', region: '南山区', firstScan: false, result: 'RISK', scene: '消费者扫码', ip: '113.87.*.*' },
]

export const certificates: CertificateRecord[] = [
  { certificateId: 'CERT-GREEN-001', name: '绿色食品认证证书', authority: '中国绿色食品发展中心', certificateNo: 'LB-03-260718001', validFrom: '2026-01-01', validTo: '2028-12-31', status: 'VALID', fileType: 'PDF' },
  { certificateId: 'CERT-ORGANIC-002', name: '有机产品认证证书', authority: '中国质量认证中心', certificateNo: 'ORG-2026-0718-02', validFrom: '2026-03-01', validTo: '2027-02-28', status: 'VALID', fileType: 'IMAGE' },
  { certificateId: 'CERT-QUALITY-003', name: '成品质检报告', authority: '黑龙江省粮油质量检测中心', certificateNo: 'HLJ-QT-20260718008', validFrom: '2026-07-17', validTo: '2027-07-16', status: 'VALID', fileType: 'PDF' },
]

export const chainProofs: ChainProofRecord[] = [
  { businessType: 'TRACE_CODE', businessId: 'RC20260718000001', dataHash: 'sha256:59e7c8347e3a6d53b672d0e7f43441ca1de9f32032d6c6aa9bd9646a3917a2ab', fileHashes: ['sha256:820f5aa3f8d6c1e7'], txId: '0x8f4aaec63d29d78c8b27f1932fe621af', blockHeight: 12888, chainTime: '2026-07-18 09:18:26', chainStatus: 'SUCCESS', verified: true, currentHash: 'sha256:59e7c8347e3a6d53b672d0e7f43441ca1de9f32032d6c6aa9bd9646a3917a2ab', chainHash: 'sha256:59e7c8347e3a6d53b672d0e7f43441ca1de9f32032d6c6aa9bd9646a3917a2ab' },
  { businessType: 'PRODUCT_BATCH', businessId: 'PROD20260701001', dataHash: 'sha256:ba3341881d77a6ef9da4aa0845b15978c6d05314939e80a66967ac496587abb2', fileHashes: ['sha256:117dfa44a0c28b33', 'sha256:40842ec9e1d79f91'], txId: '0x618731a35dcb5ef7d2518e82704f2c10', blockHeight: 12851, chainTime: '2026-07-18 08:42:10', chainStatus: 'SUCCESS', verified: true, currentHash: 'sha256:ba3341881d77a6ef9da4aa0845b15978c6d05314939e80a66967ac496587abb2', chainHash: 'sha256:ba3341881d77a6ef9da4aa0845b15978c6d05314939e80a66967ac496587abb2' },
  { businessType: 'FIELD', businessId: 'FIELD202607010003', dataHash: 'sha256:3b31f10dc85bd6a1cf021acc944d99ba7dd74e6ac2fa32827385549e71f895fa', fileHashes: [], txId: '0x90f7046418d453c2fc8b89a12a647b35', blockHeight: 12723, chainTime: '2026-07-16 17:02:45', chainStatus: 'FAILED', verified: false, currentHash: 'sha256:847af55d7a3cf92c90e1a49ad2f68f0c', chainHash: 'sha256:3b31f10dc85bd6a1cf021acc944d99ba' },
]

export const channelWarnings: ChannelWarningRecord[] = [
  { warningId: 'WARN202607260001', traceCode: 'RC20260718000002', productName: '五常有机稻花香大米', expectedRegion: '北京、天津、河北', actualRegion: '广东省深圳市', scanCount: 38, riskLevel: 'HIGH', warningTime: '2026-07-26 08:52:11', handled: false },
  { warningId: 'WARN202607250002', traceCode: 'RC20260723000005', productName: '盘锦蟹田大米', expectedRegion: '辽宁、北京', actualRegion: '浙江省杭州市', scanCount: 1, riskLevel: 'MEDIUM', warningTime: '2026-07-24 13:28:00', handled: true },
]

const baseDetail: TraceDetail = {
  traceCode: traceCodes[0]!, fieldName: '五常市民乐乡有机稻田 01 区', origin: '黑龙江省哈尔滨市五常市', harvestDate: '2026-07-10', productionDate: '2026-07-17', qualityResult: '合格',
  nutritionFacts: { 能量: '1450kJ/100g', 蛋白质: '7.2g/100g', 脂肪: '0.8g/100g', 碳水化合物: '77.9g/100g', 钠: '0mg/100g' }, certificates,
  timeline: [
    { stage: 'FIELD', title: '产地建档', time: '2026-03-02 09:00:00', organization: '五常市民乐水稻合作社', location: '五常市民乐乡', summary: '完成地块确权、GIS 边界与土壤检测信息登记', businessId: 'FIELD202603020001', chainStatus: 'SUCCESS' },
    { stage: 'PLANTING', title: '种植与农事', time: '2026-04-18 08:20:00', organization: '五常市民乐水稻合作社', location: '五常市民乐乡', summary: '稻花香2号完成播种，农事记录与投入品批次完整', businessId: 'RPB202604180001', chainStatus: 'SUCCESS' },
    { stage: 'STORAGE', title: '收储与质检', time: '2026-07-11 14:30:00', organization: '五常粮食储运中心', location: '五常市安家镇', summary: '入库净重 31.2 吨，水分、农残与重金属检测合格', businessId: 'SREC202607110001', chainStatus: 'SUCCESS' },
    { stage: 'MILLING', title: '碾米加工', time: '2026-07-15 10:10:00', organization: '稻香源米业有限公司', location: '五常市牛家工业园', summary: '精米产出率 66.4%，加工参数与出厂质检已存证', businessId: 'MB202607150001', chainStatus: 'SUCCESS' },
    { stage: 'PRODUCT', title: '包装赋码', time: '2026-07-18 09:10:00', organization: '稻香源品牌运营中心', location: '哈尔滨市道里区', summary: '完成 5kg 规格包装并激活一物一码', businessId: 'PROD20260701001', chainStatus: 'SUCCESS' },
  ],
}

export const traceDetails: Record<string, TraceDetail> = {
  RC20260718000001: baseDetail,
  RC20260718000002: { ...baseDetail, traceCode: traceCodes[1]! },
}
