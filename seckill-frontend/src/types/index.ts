/** 统一返回体，与后端 Result 对应 */
export interface Result<T = unknown> {
  code: number
  msg: string | null
  data: T
}

/** 业务错误，携带后端错误码 */
export class ApiError extends Error {
  code: number

  constructor(code: number, msg: string) {
    super(msg)
    this.name = 'ApiError'
    this.code = code
  }
}

/* ---------- 用户 ---------- */

export interface LoginPayload {
  phone: string
  password: string
}

export interface RegisterPayload {
  email: string
  username: string
  password: string
  phone: string
  age: number
  sex: string
}

export interface TokenData {
  token: string
}

/** 对应后端 UserInfoVO，role: 0 超管 / 1 管理员 / 2 普通用户；status: 1 正常 / 0 封禁 */
export interface UserInfo {
  id: number
  username: string
  sex: string
  age: number
  phone: string
  email: string
  role: number
  status: number
}

/* ---------- 商品 ---------- */

/** 对应后端 ProductsVO，时间为 ISO 字符串 */
export interface Product {
  id: number
  name: string
  description: string | null
  price: number
  status: number
  imageUrl: string | null
  createTime: string
  updateTime: string
}

export interface ProductPayload {
  name: string
  description?: string
  price: number
  imageUrl?: string
}
