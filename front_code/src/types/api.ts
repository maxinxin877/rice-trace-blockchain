/** 统一 API 响应 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页响应 */
export interface PageResponse<T = unknown> {
  code: number
  message: string
  data: {
    records: T[]
    total: number
    page: number
    pageSize: number
    totalPages: number
  }
}

/** 分页查询参数 */
export interface PageQuery {
  page?: number
  pageSize?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}
