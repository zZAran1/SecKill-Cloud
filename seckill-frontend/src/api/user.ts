import http from './http'
import type {
  LoginPayload,
  RegisterPayload,
  Result,
  TokenData,
  UserInfo,
} from '@/types'

/** 用户服务接口（8081，经 /api 代理） */
export const userApi = {
  register: (data: RegisterPayload) => http.post<unknown, Result<null>>('/users', data),

  login: (data: LoginPayload) => http.post<unknown, Result<TokenData>>('/users/login', data),

  me: () => http.get<unknown, Result<UserInfo>>('/users/me'),

  logout: () => http.post<unknown, Result<null>>('/users/logout'),
}
