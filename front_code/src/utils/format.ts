/**
 * 格式化日期时间
 */
export function formatDateTime(date: string | Date, format = 'YYYY-MM-DD HH:mm:ss'): string {
  const d = typeof date === 'string' ? new Date(date) : date
  if (isNaN(d.getTime())) return '-'

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化日期
 */
export function formatDate(date: string | Date): string {
  return formatDateTime(date, 'YYYY-MM-DD')
}

/**
 * 格式化面积（亩）
 */
export function formatArea(value: number | undefined | null): string {
  if (value == null) return '-'
  return `${value.toFixed(2)} 亩`
}

/**
 * 格式化重量（kg）
 */
export function formatWeight(value: number | undefined | null): string {
  if (value == null) return '-'
  return `${value.toFixed(2)} kg`
}

/**
 * 格式化百分比
 */
export function formatPercent(value: number | undefined | null): string {
  if (value == null) return '-'
  return `${value.toFixed(2)}%`
}
