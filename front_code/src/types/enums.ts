/** 种植批次状态 */
export enum RiceBatchStatus {
  PLANTED = 'PLANTED',
  HARVESTED = 'HARVESTED',
  STORED = 'STORED',
  MILLING = 'MILLING',
  COMPLETED = 'COMPLETED',
}

/** 农事操作类型 */
export enum FarmingOperationType {
  SOWING = 'SOWING',
  FERTILIZING = 'FERTILIZING',
  PESTICIDE = 'PESTICIDE',
  IRRIGATION = 'IRRIGATION',
  WEEDING = 'WEEDING',
  HARVESTING = 'HARVESTING',
  OTHER = 'OTHER',
}

/** 质检结果 */
export enum QualityResult {
  PASS = 'PASS',
  FAIL = 'FAIL',
}

/** 防伪码状态 */
export enum TraceCodeStatus {
  GENERATED = 'GENERATED',
  ACTIVATED = 'ACTIVATED',
  DEACTIVATED = 'DEACTIVATED',
  RISK = 'RISK',
}

/** 链上状态 */
export enum ChainStatus {
  PENDING = 'PENDING',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
}

/** 风险预警类型 */
export enum RiskWarningType {
  YIELD_BALANCE = 'YIELD_BALANCE',
  CHANNEL_CONFLICT = 'CHANNEL_CONFLICT',
  REPEAT_SCAN = 'REPEAT_SCAN',
  UNACTIVATED_SCAN = 'UNACTIVATED_SCAN',
  CHAIN_VERIFY_FAILED = 'CHAIN_VERIFY_FAILED',
}

/** 风险等级 */
export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
}

/** 环境数据来源 */
export enum EnvironmentSourceType {
  IOT = 'IOT',
  DRONE = 'DRONE',
  MANUAL = 'MANUAL',
}

/** 质检业务类型 */
export enum QualityBusinessType {
  STORAGE = 'STORAGE',
  MILLING = 'MILLING',
}
