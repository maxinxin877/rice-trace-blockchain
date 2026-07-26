import {
  RiceBatchStatus,
  FarmingOperationType,
  QualityResult,
  TraceCodeStatus,
  ChainStatus,
  RiskWarningType,
  RiskLevel,
  EnvironmentSourceType,
  QualityBusinessType,
} from '@/types/enums'

/** 种植批次状态映射 */
export const RICE_BATCH_STATUS_MAP: Record<string, { label: string; color: string }> = {
  [RiceBatchStatus.PLANTED]: { label: '种植中', color: '#409EFF' },
  [RiceBatchStatus.HARVESTED]: { label: '已收割', color: '#E6A23C' },
  [RiceBatchStatus.STORED]: { label: '已入库', color: '#67C23A' },
  [RiceBatchStatus.MILLING]: { label: '加工中', color: '#909399' },
  [RiceBatchStatus.COMPLETED]: { label: '已完成', color: '#67C23A' },
}

/** 农事操作类型映射 */
export const FARMING_OPERATION_TYPE_MAP: Record<string, { label: string; color: string }> = {
  [FarmingOperationType.SOWING]: { label: '播种', color: '#409EFF' },
  [FarmingOperationType.FERTILIZING]: { label: '施肥', color: '#67C23A' },
  [FarmingOperationType.PESTICIDE]: { label: '用药', color: '#E6A23C' },
  [FarmingOperationType.IRRIGATION]: { label: '灌溉', color: '#00BCD4' },
  [FarmingOperationType.WEEDING]: { label: '除草', color: '#909399' },
  [FarmingOperationType.HARVESTING]: { label: '收割', color: '#FF9800' },
  [FarmingOperationType.OTHER]: { label: '其他', color: '#909399' },
}

/** 质检结果映射 */
export const QUALITY_RESULT_MAP: Record<string, { label: string; color: string }> = {
  [QualityResult.PASS]: { label: '合格', color: '#67C23A' },
  [QualityResult.FAIL]: { label: '不合格', color: '#F56C6C' },
}

/** 防伪码状态映射 */
export const TRACE_CODE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  [TraceCodeStatus.GENERATED]: { label: '已生成', color: '#909399' },
  [TraceCodeStatus.ACTIVATED]: { label: '已激活', color: '#67C23A' },
  [TraceCodeStatus.DEACTIVATED]: { label: '已停用', color: '#F56C6C' },
  [TraceCodeStatus.RISK]: { label: '风险', color: '#E6A23C' },
}

/** 链上状态映射 */
export const CHAIN_STATUS_MAP: Record<string, { label: string; color: string }> = {
  [ChainStatus.PENDING]: { label: '待上链', color: '#909399' },
  [ChainStatus.SUCCESS]: { label: '已上链', color: '#67C23A' },
  [ChainStatus.FAILED]: { label: '上链失败', color: '#F56C6C' },
}

/** 风险预警类型映射 */
export const RISK_WARNING_TYPE_MAP: Record<string, string> = {
  [RiskWarningType.YIELD_BALANCE]: '产量平衡异常',
  [RiskWarningType.CHANNEL_CONFLICT]: '窜货预警',
  [RiskWarningType.REPEAT_SCAN]: '重复扫码',
  [RiskWarningType.UNACTIVATED_SCAN]: '未激活扫码',
  [RiskWarningType.CHAIN_VERIFY_FAILED]: '链上核验失败',
}

/** 风险等级映射 */
export const RISK_LEVEL_MAP: Record<string, { label: string; color: string }> = {
  [RiskLevel.LOW]: { label: '低风险', color: '#909399' },
  [RiskLevel.MEDIUM]: { label: '中风险', color: '#E6A23C' },
  [RiskLevel.HIGH]: { label: '高风险', color: '#F56C6C' },
}

/** 环境数据来源映射 */
export const ENVIRONMENT_SOURCE_TYPE_MAP: Record<string, { label: string; color: string }> = {
  [EnvironmentSourceType.IOT]: { label: '物联网设备', color: '#409EFF' },
  [EnvironmentSourceType.DRONE]: { label: '无人机', color: '#67C23A' },
  [EnvironmentSourceType.MANUAL]: { label: '人工记录', color: '#909399' },
}

/** 质检业务类型映射 */
export const QUALITY_BUSINESS_TYPE_MAP: Record<string, string> = {
  [QualityBusinessType.STORAGE]: '入库质检',
  [QualityBusinessType.MILLING]: '加工质检',
}

/** 省市区级联数据（示例） */
export const REGION_DATA = [
  {
    value: '110000',
    label: '北京市',
    children: [
      { value: '110100', label: '北京市', children: [{ value: '110101', label: '东城区' }, { value: '110102', label: '西城区' }] },
    ],
  },
  {
    value: '230000',
    label: '黑龙江省',
    children: [
      {
        value: '230100',
        label: '哈尔滨市',
        children: [
          { value: '230101', label: '道里区' },
          { value: '230102', label: '南岗区' },
          { value: '230103', label: '道外区' },
        ],
      },
      {
        value: '230200',
        label: '齐齐哈尔市',
        children: [
          { value: '230201', label: '龙沙区' },
          { value: '230202', label: '建华区' },
        ],
      },
      {
        value: '230300',
        label: '五常市',
        children: [{ value: '230301', label: '五常镇' }],
      },
    ],
  },
  {
    value: '220000',
    label: '吉林省',
    children: [
      {
        value: '220100',
        label: '长春市',
        children: [{ value: '220101', label: '南关区' }, { value: '220102', label: '宽城区' }],
      },
      {
        value: '220200',
        label: '吉林市',
        children: [{ value: '220201', label: '昌邑区' }, { value: '220202', label: '龙潭区' }],
      },
    ],
  },
  {
    value: '210000',
    label: '辽宁省',
    children: [
      {
        value: '210100',
        label: '沈阳市',
        children: [{ value: '210101', label: '和平区' }, { value: '210102', label: '沈河区' }],
      },
      {
        value: '210200',
        label: '盘锦市',
        children: [{ value: '210201', label: '双台子区' }, { value: '210202', label: '兴隆台区' }],
      },
    ],
  },
]
