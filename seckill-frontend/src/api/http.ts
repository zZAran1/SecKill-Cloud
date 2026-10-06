import axios, { AxiosError } from 'axios'
import type { Result } from '@/types'
import { ApiError } from '@/types'

export const TOKEN_KEY = 'seckill.token'

/** 登录态失效时的回调（由 main.ts 注入，负责清理状态并视情况跳转登录页） */
let onUnauthorized: (() => void) | null = null
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
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
      if (body.code === 401) onUnauthorized?.()
      throw new ApiError(body.code, body.msg || '请求失败')
    }
    return body as never
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const body: unknown = error.response?.data

    if (status === 401) {
      // 仅当返回体是后端 Result 结构时才判定为登录态失效；
      // 裸 401（无业务体）多为服务侧拦截，不清空本地登录态
      if (isResultBody(body)) {
        onUnauthorized?.()
        throw new ApiError(401, body.msg || '未登录或登录已过期')
      }
      throw new ApiError(401, '访问被拒绝，请稍后重试')
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
