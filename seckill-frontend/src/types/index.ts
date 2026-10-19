/** 统一返回体，与后端 Result 对应 */
export interface Result<T = unknown> {
  code: number
  msg: string | null
  data: T
}

/** 统一分页返回体，与后端 PageResult 对应 */
export interface PageResult<T = unknown> {
  records: T[]
  total: number
  page: number
  size: number
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
  address: string | null
  role: number
  status: number
  createTime: string
}

/** 个人资料修改，字段可选，只更新传入的字段 */
export interface UpdateProfilePayload {
  username?: string
  email?: string
  sex?: string
  age?: number
  address?: string
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export interface UpdateUserStatusPayload {
  status: number
}

/* ---------- 商品 ---------- */

/** 对应后端 ProductsVO，时间为 ISO 字符串；status: 1 在售 / 0 下架 */
export interface Product {
  id: number
  name: string
  description: string | null
  price: number
  status: number
  stock: number
  imageUrl: string | null
  createTime: string
  updateTime: string
}

export interface ProductPayload {
  name: string
  description?: string
  price: number
  imageUrl?: string
  stock: number
}

/** 商品编辑，字段可选，只更新传入的字段 */
export interface UpdateProductPayload {
  name?: string
  description?: string
  price?: number
  imageUrl?: string
  stock?: number
}

export interface ProductStatusPayload {
  status: number
}

/* ---------- 秒杀活动 ---------- */

/** 对应后端 SeckillActivityVO；status: 1 上线 / 0 下线 */
export interface SeckillActivity {
  id: number
  productId: number
  productName: string | null
  productImage: string | null
  seckillPrice: number
  originalPrice: number | null
  /** 秒杀专用库存 */
  stock: number
  /** 实时剩余库存 */
  remainingStock: number
  startTime: string
  endTime: string
  status: number
  createTime: string
}

/** 创建活动（后端要求 startTime 必须晚于当前时间） */
export interface ActivityPayload {
  productId: number
  seckillPrice: number
  stock: number
  startTime: string
  endTime: string
}

/** 修改活动，不允许改关联商品，仅未开始的活动可改 */
export interface UpdateActivityPayload {
  seckillPrice: number
  stock: number
  startTime: string
  endTime: string
}

/** 对应后端 SeckillResultVO，result: 0 排队中 / 1 抢购成功 / 2 未抢到 */
export interface SeckillResult {
  result: number
  message: string
  orderNo: string | null
  orderStatus: number | null
}

/* ---------- 订单 ---------- */

/** 对应后端 OrderVO，status: 0 待支付 / 1 已支付 / 2 已取消 */
export interface Order {
  orderNo: string
  userId: number
  productId: number
  /** 秒杀活动 ID，普通订单为 null */
  activityId: number | null
  quantity: number
  totalAmount: number
  status: number
  statusDesc: string
  /** 支付截止时间 */
  expireTime: string | null
  createTime: string
  updateTime: string
}

export interface CreateOrderPayload {
  productId: number
  quantity: number
}
