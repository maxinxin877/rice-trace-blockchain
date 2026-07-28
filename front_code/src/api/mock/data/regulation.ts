import type { AuditLogRecord, RiskWarningRecord, YieldBalanceResult } from '@/types/regulation'

export const yieldBalanceResults: YieldBalanceResult[] = [
  {
    checkId: 'YBC202607260001', plantingBatchId: 'RPB202607010001', grainBatchId: 'GRAIN202607110001', productBatchId: 'PROD20260701001', checkScope: 'FULL_CHAIN', result: 'PASS', chainStatus: 'SUCCESS', checkedAt: '2026-07-26 10:15:00',
    items: [
      { itemName: '亩产校验', inputValue: 120.5, outputValue: 48200, computedValue: 400, unit: 'kg/亩', threshold: '300-700kg/亩', result: 'PASS' },
      { itemName: '精米产出率', inputValue: 10000, outputValue: 6640, computedValue: 66.4, unit: '%', threshold: '55%-75%', result: 'PASS' },
    ],
  },
  {
    checkId: 'YBC202607250002', plantingBatchId: 'RPB202607010003', grainBatchId: 'GRAIN202607160003', productBatchId: 'PROD20260701003', checkScope: 'FULL_CHAIN', result: 'FAIL', chainStatus: 'PENDING', checkedAt: '2026-07-25 16:40:00',
    items: [
      { itemName: '亩产校验', inputValue: 80, outputValue: 22400, computedValue: 280, unit: 'kg/亩', threshold: '300-700kg/亩', result: 'FAIL' },
      { itemName: '精米产出率', inputValue: 8000, outputValue: 4880, computedValue: 61, unit: '%', threshold: '55%-75%', result: 'PASS' },
    ],
  },
]

export const riskWarnings: RiskWarningRecord[] = [
  { warningId: 'WARN202607260001', warningType: 'CHANNEL_CONFLICT', riskLevel: 'HIGH', businessType: 'TRACE_CODE', businessId: 'RC20260718000002', warningContent: '防伪码预期在北京、天津、河北销售，但在广东省深圳市出现 38 次扫码，疑似跨区域窜货', handled: false, handledBy: null, handledAt: null, handleResult: null, chainStatus: 'PENDING', createdAt: '2026-07-26 08:52:11' },
  { warningId: 'WARN202607250002', warningType: 'REPEAT_SCAN', riskLevel: 'HIGH', businessType: 'TRACE_CODE', businessId: 'RC20260718000001', warningContent: '同一防伪码在短时间内出现多次重复扫码，请核对销售渠道', handled: false, handledBy: null, handledAt: null, handleResult: null, chainStatus: 'SUCCESS', createdAt: '2026-07-25 17:00:00' },
  { warningId: 'WARN202607250003', warningType: 'YIELD_BALANCE', riskLevel: 'MEDIUM', businessType: 'PLANTING_BATCH', businessId: 'RPB202607010003', warningContent: '亩产计算值 280kg/亩，低于 300kg/亩阈值', handled: false, handledBy: null, handledAt: null, handleResult: null, chainStatus: 'PENDING', createdAt: '2026-07-25 16:40:00' },
  { warningId: 'WARN202607200004', warningType: 'CHAIN_VERIFY_FAILED', riskLevel: 'HIGH', businessType: 'FIELD', businessId: 'FIELD202607010003', warningContent: '地块数据哈希与链上存证不一致，可能存在数据篡改', handled: true, handledBy: 'ADMIN001', handledAt: '2026-07-20 09:10:00', handleResult: '已核对原始记录并重新提交链上存证', chainStatus: 'SUCCESS', createdAt: '2026-07-20 08:00:00' },
]

export const auditLogs: AuditLogRecord[] = [
  { auditId: 'AUDIT202607260001', businessType: 'TRACE_CODE', businessId: 'RC20260718000002', operationType: 'VERIFY', operatorId: 'SYSTEM', operatorName: '风控引擎', beforeHash: null, afterHash: 'sha256:trace-risk-002', reason: '跨区域扫码触发风险预警', ip: '10.0.3.20', operationTime: '2026-07-26 08:52:11', txId: '0xtrace002' },
  { auditId: 'AUDIT202607250002', businessType: 'PLANTING_BATCH', businessId: 'RPB202607010003', operationType: 'VERIFY', operatorId: 'REGULATOR001', operatorName: '监管员王敏', beforeHash: 'sha256:yield-before', afterHash: 'sha256:yield-after', reason: '执行产量平衡校验', ip: '10.0.4.12', operationTime: '2026-07-25 16:40:00', txId: '0xyield003' },
  { auditId: 'AUDIT202607200003', businessType: 'FIELD', businessId: 'FIELD202607010003', operationType: 'HANDLE', operatorId: 'ADMIN001', operatorName: '系统管理员', beforeHash: 'sha256:field-old', afterHash: 'sha256:field-fixed', reason: '修复链上哈希不一致', ip: '192.168.1.100', operationTime: '2026-07-20 09:10:00', txId: '0xfield003' },
  { auditId: 'AUDIT202607180004', businessType: 'PRODUCT_BATCH', businessId: 'PROD20260701001', operationType: 'CREATE', operatorId: 'BRAND_STAFF001', operatorName: '品牌运营李佳', beforeHash: null, afterHash: 'sha256:product001', reason: '完成包装并生成防伪码', ip: '10.0.2.10', operationTime: '2026-07-18 09:10:00', txId: '0xproduct001' },
]
