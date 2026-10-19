import http from './http'
import type {
  ChangePasswordPayload,
  LoginPayload,
  PageResult,
  RegisterPayload,
  Result,
  TokenData,
  UpdateProfilePayload,
  UpdateUserStatusPayload,
  UserInfo,
} from '@/types'

/** 用户服务接口（8081，经 /api → 网关转发） */
export const userApi = {
  register: (data: RegisterPayload) => http.post<unknown, Result<null>>('/users', data),

  login: (data: LoginPayload) => http.post<unknown, Result<TokenData>>('/users/login', data),

  me: () => http.get<unknown, Result<UserInfo>>('/users/me'),

  logout: () => http.post<unknown, Result<null>>('/users/logout'),

  /** 修改个人资料，字段可选，只更新传入的字段 */
  updateProfile: (data: UpdateProfilePayload) =>
    http.put<unknown, Result<UserInfo>>('/users/me', data),

  changePassword: (data: ChangePasswordPayload) =>
    http.put<unknown, Result<null>>('/users/password', data),

  /** 管理员：分页查询用户 */
  pageUsers: (params: { keyword?: string; page?: number; size?: number }) =>
    http.get<unknown, Result<PageResult<UserInfo>>>('/users/page', { params }),

  /** 管理员：封禁（status=0）/ 解封（status=1） */
  updateUserStatus: (id: number, data: UpdateUserStatusPayload) =>
    http.put<unknown, Result<null>>(`/users/${id}/status`, data),
}
