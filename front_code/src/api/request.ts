import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // 预留：从 store 获取 token 并附加到请求头
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 后端分页参数名为 pageNo，前端统一使用 page
    if (config.params && config.params.page !== undefined && config.params.pageNo === undefined) {
      config.params.pageNo = config.params.page
      delete config.params.page
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 文件/blob 下载响应直接放行（不做 JSON 归一化）
    if (response.config.responseType === 'blob') return response
    const res = response.data
    if (res.code === 200 || res.code === 0) {
      // 后端成功码为 0，统一为前端约定的 200
      if (res.code === 0) res.code = 200
      // 后端分页字段为 pageNo，统一为前端约定的 page/totalPages
      const data = res.data as { records?: unknown[]; pageNo?: number; pageSize?: number; total?: number } | null
      if (data && Array.isArray(data.records)) {
        const pageNo = data.pageNo ?? 1
        const pageSize = data.pageSize ?? 10
        ;(data as Record<string, unknown>).page = pageNo
        ;(data as Record<string, unknown>).totalPages = Math.ceil((data.total ?? 0) / pageSize)
      }
      return response
    }
    // 业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          // 清除本地登录态并跳转登录页（已在登录页则不重复跳转）
          localStorage.removeItem('token')
          localStorage.removeItem('role')
          localStorage.removeItem('userName')
          if (!window.location.hash.startsWith('#/login')) {
            window.location.hash = '#/login'
          }
          break
        case 403:
          ElMessage.error('无权访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(`请求失败 (${status})`)
      }
    } else {
      ElMessage.error('网络连接异常')
    }
    return Promise.reject(error)
  }
)

/** 通用 GET 请求 */
export function get<T = unknown>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
  return instance.get(url, { params, ...config })
}

/** 通用 POST 请求 */
export function post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
  return instance.post(url, data, config)
}

/** 通用 PUT 请求 */
export function put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
  return instance.put(url, data, config)
}

/** 通用 DELETE 请求 */
export function del<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
  return instance.delete(url, config)
}

/** 以 blob 形式下载文件（下载接口需鉴权，返回原始二进制） */
export async function getBlob(url: string, config?: AxiosRequestConfig): Promise<Blob> {
  const res = await instance.get(url, { ...config, responseType: 'blob' })
  return res.data as Blob
}

export default instance
