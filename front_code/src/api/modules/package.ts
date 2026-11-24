/**
 * 箱码/托盘码 API（后端 RicePackageUnitController）
 */
import type { ApiResponse } from '@/types/api'
import type { PackageCreatePayload, PackageDetail, PackageUnit } from '@/types/package'
import { get, post } from '../request'

export const packageApi = {
  async create(payload: PackageCreatePayload): Promise<ApiResponse<PackageUnit>> {
    return (await post<PackageUnit>('/rice/packages', payload)).data
  },

  async getList(params: Record<string, unknown>) {
    return (await get<{ records: PackageUnit[]; total: number }>('/rice/packages', params)).data
  },

  async detail(unitCode: string): Promise<ApiResponse<PackageDetail>> {
    return (await get<PackageDetail>(`/rice/packages/${unitCode}`)).data
  },

  async expand(unitCode: string): Promise<ApiResponse<string[]>> {
    return (await get<string[]>(`/rice/packages/${unitCode}/trace-codes`)).data
  },

  async upward(traceCode: string): Promise<ApiResponse<Record<string, unknown>>> {
    return (await get<Record<string, unknown>>(`/rice/packages/trace-code/${traceCode}/upward`)).data
  },
}
