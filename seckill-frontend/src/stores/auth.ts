import { computed, reactive } from 'vue'
import { userApi } from '@/api/user'
import { clearToken, getToken, setRefreshToken, setToken } from '@/utils/security'
import type { LoginPayload, RegisterPayload, UserInfo } from '@/types'

const USER_KEY = 'seckill.user'

interface AuthState {
  token: string | null
  user: UserInfo | null
}

function readUser(): UserInfo | null {
  try {
    const raw = sessionStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

const state = reactive<AuthState>({
  token: getToken(),
  user: readUser(),
})

function persist() {
  if (state.token) setToken(state.token)
  else clearToken()

  if (state.user) sessionStorage.setItem(USER_KEY, JSON.stringify(state.user))
  else sessionStorage.removeItem(USER_KEY)
}

/** 登录态 store（模块级单例，避免额外引入状态库） */
export function useAuth() {
  const isLoggedIn = computed(() => !!state.token)
  const isAdmin = computed(() => state.user !== null && state.user.role <= 1)

  async function login(payload: LoginPayload) {
    const res = await userApi.login(payload)
    state.token = res.data.token
    persist()
    // 长时效 refresh token 用于 access 过期后静默续期
    setRefreshToken(res.data.refreshToken)
    try {
      await fetchUser()
    } catch {
      /* 用户信息拉取失败不阻断登录 */
    }
  }

  async function register(payload: RegisterPayload) {
    await userApi.register(payload)
  }

  async function fetchUser() {
    const res = await userApi.me()
    state.user = res.data
    persist()
  }

  async function logout() {
    if (state.token) {
      try {
        await userApi.logout()
      } catch {
        /* token 可能已过期，本地照常清理 */
      }
    }
    clear()
  }

  function clear() {
    state.token = null
    state.user = null
    persist()
  }

  return { state, isLoggedIn, isAdmin, login, register, fetchUser, logout, clear }
}
