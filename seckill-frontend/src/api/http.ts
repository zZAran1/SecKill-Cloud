import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { Result, TokenData } from '@/types'
import { ApiError } from '@/types'
import {
  clearToken,
  getRefreshToken,
  getToken,
  isTokenExpired,
  setRefreshToken,
  setToken,
} from '@/utils/security'

/** 登录态失效时的回调（由 main.ts 注入，负责清理状态并视情况跳转登录页） */
let onUnauthorized: (() => void) | null = null
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 统一经网关（8080）访问：鉴权、验签、限流全部由网关收敛
const http = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
})

/** 刷新请求用独立实例，避免走 http 拦截器造成递归 */
const refreshHttp = axios.create({ baseURL: BASE_URL, timeout: 15000 })

interface RetryConfig extends AxiosRequestConfig {
  /** 标记已重放过，防止 401 无限重试 */
  _retried?: boolean
}

/** 单飞刷新：并发多个请求同时 401 时，只发起一次刷新 */
let refreshPromise: Promise<string | null> | null = null

/** 用 refreshToken 换新 token 对；失败返回 null（登录态真正失效） */
async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = getRefreshToken()
      if (!refreshToken) return null
      try {
        const res = await refreshHttp.post<Result<TokenData>>('/users/refresh', { refreshToken })
        const body = res.data
        if (body.code !== 200 || !body.data?.token) return null
        setToken(body.data.token)
        setRefreshToken(body.data.refreshToken)
        return body.data.token
      } catch {
        return null
      }
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

/** 401 统一处理：先尝试静默刷新并重放原请求一次，失败才判定登录态失效 */
async function handleUnauthorized(config: RetryConfig, isResultBody: boolean): Promise<unknown> {
  const fresh = await refreshAccessToken()
  if (fresh && !config._retried) {
    config._retried = true
    config.headers = { ...config.headers, Authorization: `Bearer ${fresh}` }
    return http.request(config)
  }
  // 刷新失败或重放后仍 401：清理登录态
  if (isResultBody || fresh) {
    clearToken()
    onUnauthorized?.()
    throw new ApiError(401, '登录已过期，请重新登录')
  }
  // 裸 401（无业务体）多为服务侧拦截，保留本地登录态
  throw new ApiError(401, '访问被拒绝，请稍后重试')
}

http.interceptors.request.use(async (config) => {
  const token = getToken()
  if (token) {
    // 本地先行检测过期：有 refreshToken 就静默续期，避免发出必然失败的请求
    if (isTokenExpired(token)) {
      const fresh = await refreshAccessToken()
      if (!fresh) {
        clearToken()
        onUnauthorized?.()
        return Promise.reject(new ApiError(401, '登录已过期，请重新登录'))
      }
      config.headers.Authorization = `Bearer ${fresh}`
      return config
    }
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function isResultBody(body: unknown): body is Result {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as Result).code === 'number'
  )
}

http.interceptors.response.use(
  (response) => {
    const body: unknown = response.data
    if (isResultBody(body)) {
      if (body.code === 200) return body as never
      if (body.code === 401) {
        return handleUnauthorized(response.config as RetryConfig, true) as never
      }
      throw new ApiError(body.code, body.msg || '请求失败')
    }
    return body as never
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const body: unknown = error.response?.data

    if (status === 401) {
      return handleUnauthorized((error.config ?? {}) as RetryConfig, isResultBody(body)) as never
    }

    if (status === 429) {
      throw new ApiError(429, '操作过于频繁，请稍后再试')
    }

    if (isResultBody(body)) {
      throw new ApiError(body.code, body.msg || '请求失败')
    }

    if (error.code === 'ECONNABORTED') {
      throw new ApiError(-1, '请求超时，请稍后再试')
    }
    throw new ApiError(status ?? -1, '网络异常，请确认后端服务已启动')
  },
)

export default http
