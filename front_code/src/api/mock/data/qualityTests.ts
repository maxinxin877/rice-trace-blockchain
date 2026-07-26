import type { RiceQualityTest } from '@/types/qualityTest'
import { QualityBusinessType, QualityResult, ChainStatus } from '@/types/enums'

export const mockQualityTests: RiceQualityTest[] = [
  {
    qualityTestId: 'QTEST202607010001',
    tenantId: 'TENANT001',
    businessType: QualityBusinessType.STORAGE,
    businessId: 'SREC202607010001',
    testAgency: '黑龙江省粮油质量检测中心',
    testTime: '2026-09-21T10:00:00',
    overallResult: QualityResult.PASS,
    reportFileId: 'file_report_001',
    reportHash: 'sha256:report001hash',
    remark: '各项指标符合国家标准',
    dataHash: 'sha256:qtest001hash',
    chainStatus: ChainStatus.SUCCESS,
    txId: '0xqtest001',
    createdBy: 'TESTER001',
    createdAt: '2026-09-21T10:30:00',
    items: [
      { itemId: 'ITEM001', qualityTestId: 'QTEST202607010001', itemName: '水分', itemValue: 14.50, unit: '%', standardValue: '≤15.5', result: QualityResult.PASS },
      { itemId: 'ITEM002', qualityTestId: 'QTEST202607010001', itemName: '杂质', itemValue: 0.80, unit: '%', standardValue: '≤1.0', result: QualityResult.PASS },
      { itemId: 'ITEM003', qualityTestId: 'QTEST202607010001', itemName: '出糙率', itemValue: 79.00, unit: '%', standardValue: '≥77', result: QualityResult.PASS },
      { itemId: 'ITEM004', qualityTestId: 'QTEST202607010001', itemName: '整精米率', itemValue: 58.00, unit: '%', standardValue: '≥55', result: QualityResult.PASS },
      { itemId: 'ITEM005', qualityTestId: 'QTEST202607010001', itemName: '黄粒米', itemValue: 0.20, unit: '%', standardValue: '≤1.0', result: QualityResult.PASS },
    ],
  },
  {
    qualityTestId: 'QTEST202607010002',
    tenantId: 'TENANT001',
    businessType: QualityBusinessType.STORAGE,
    businessId: 'SREC202607010003',
    testAgency: '吉林省粮油质量检测中心',
    testTime: '2026-09-21T14:00:00',
    overallResult: QualityResult.FAIL,
    reportFileId: 'file_report_002',
    reportHash: 'sha256:report002hash',
    remark: '杂质含量超标，黄粒米比例偏高',
    chainStatus: ChainStatus.PENDING,
    createdBy: 'TESTER002',
    createdAt: '2026-09-21T14:30:00',
    items: [
      { itemId: 'ITEM006', qualityTestId: 'QTEST202607010002', itemName: '水分', itemValue: 15.00, unit: '%', standardValue: '≤15.5', result: QualityResult.PASS },
      { itemId: 'ITEM007', qualityTestId: 'QTEST202607010002', itemName: '杂质', itemValue: 1.50, unit: '%', standardValue: '≤1.0', result: QualityResult.FAIL },
      { itemId: 'ITEM008', qualityTestId: 'QTEST202607010002', itemName: '黄粒米', itemValue: 1.50, unit: '%', standardValue: '≤1.0', result: QualityResult.FAIL },
    ],
  },
]
