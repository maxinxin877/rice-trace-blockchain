import { get, post } from '@/api/request'

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
}

/** 注册请求参数 */
export interface RegisterParams {
  username: string
  password: string
  nickname?: string
  phone?: string
  role?: string
}

/** 登录返回的用户信息 */
export interface LoginUser {
  id: number
  username: string
  nickname: string
  phone?: string
  role: string
  status: number
}

/** 登录返回 */
export interface LoginResult {
  token: string
  expiresIn: number
  user: LoginUser
}

/**
 * 认证 API（对接后端 /api/v1/auth/*）
 */
export const authApi = {
  /** 登录 */
  login(params: LoginParams) {
    return post<LoginResult>('/auth/login', params)
  },

  /** 注册 */
  register(params: RegisterParams) {
    return post<void>('/auth/register', params)
  },

  /** 获取当前登录用户信息 */
  getMe() {
    return get<LoginUser>('/auth/me')
  },

  /** 退出登录 */
  logout() {
    return post<void>('/auth/logout')
  },
}
