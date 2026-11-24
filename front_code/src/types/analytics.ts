/** 质量分析类型（对应后端 /rice/analytics/**） */

export interface QualityTrendItem {
  month: string
  passCount: number
  failCount: number
  passRate: number
}

export interface WarningDistributionItem {
  type: string
  label: string
  count: number
  unhandled: number
}

export interface RecallPerformance {
  taskCount: number
  releasedCount: number
  closedCount: number
  closeRate: number
  avgHandleHours: number
  recoveryRate: number
  itemCount: number
  unRecoveredCount: number
}

export interface RiskProfile {
  score: number
  level: 'LOW' | 'MEDIUM' | 'HIGH'
  factors: string[]
  suggestions: string[]
  calculatedAt: string
  method: string
}

export interface AnalyticsOverview {
  qualityTrend: QualityTrendItem[]
  warningDistribution: WarningDistributionItem[]
  recallPerformance: RecallPerformance
  riskProfile: RiskProfile
}

export const RISK_LEVEL_LABEL: Record<string, string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
}
