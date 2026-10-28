/**
 * 前端安全工具
 * - 令牌采用 Bearer 头传输（不依赖 Cookie），天然免疫 CSRF
 * - 令牌存于 sessionStorage（会话级）+ 内存缓存，降低 XSS 下的持久化泄露风险
 * - 提供 JWT 过期检测、图片协议白名单、站内跳转校验
 */

const TOKEN_KEY = 'seckill.token'
const REFRESH_KEY = 'seckill.refresh'

let memoryToken: string | null = null
let memoryRefresh: string | null = null

export function getToken(): string | null {
  if (memoryToken) return memoryToken
  memoryToken = sessionStorage.getItem(TOKEN_KEY)
  return memoryToken
}

export function setToken(token: string) {
  memoryToken = token
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function getRefreshToken(): string | null {
  if (memoryRefresh) return memoryRefresh
  memoryRefresh = sessionStorage.getItem(REFRESH_KEY)
  return memoryRefresh
}

export function setRefreshToken(refreshToken: string) {
  memoryRefresh = refreshToken
  sessionStorage.setItem(REFRESH_KEY, refreshToken)
}

/** 清空登录态（access + refresh 一起清） */
export function clearToken() {
  memoryToken = null
  memoryRefresh = null
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
}

/** 解析 JWT 载荷中的过期时间（秒），解析失败返回 null */
export function readTokenExp(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join(''),
    )
    const data = JSON.parse(json) as { exp?: number }
    return typeof data.exp === 'number' ? data.exp : null
  } catch {
    return null
  }
}

/** 令牌是否已过期（预留 5 秒时钟偏移容错）；无法解析时不拦截，交由后端校验 */
export function isTokenExpired(token: string): boolean {
  const exp = readTokenExp(token)
  if (exp === null) return false
  return exp * 1000 <= Date.now() + 5000
}

/** 图片地址安全校验：仅放行 http/https，拦截 javascript: 等脚本协议 */
export function safeImageUrl(url: string | null | undefined): string | null {
  if (!url) return null
  const trimmed = url.trim()
  return /^https?:\/\//i.test(trimmed) ? trimmed : null
}

/** 站内跳转校验：防开放重定向，仅允许以单个 / 开头的相对路径 */
export function isSafeRedirect(path: unknown): path is string {
  return (
    typeof path === 'string' &&
    path.startsWith('/') &&
    !path.startsWith('//') &&
    !path.includes('\\')
  )
}
