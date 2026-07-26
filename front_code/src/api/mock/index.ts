/**
 * Mock 工具函数
 */

/** 模拟网络延迟 */
export function mockDelay(ms = 300): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** 模拟分页结果 */
export function mockPaginate<T>(list: T[], page = 1, pageSize = 10) {
  const total = list.length
  const start = (page - 1) * pageSize
  const end = start + pageSize
  const records = list.slice(start, end)
  const totalPages = Math.ceil(total / pageSize)
  return {
    records,
    total,
    page,
    pageSize,
    totalPages,
  }
}

/** 生成模拟 ID */
export function mockId(prefix: string): string {
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
  const random = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
  return `${prefix}${dateStr}${random}`
}
