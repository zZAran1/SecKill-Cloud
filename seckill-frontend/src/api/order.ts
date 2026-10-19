import http from './http'
import type { CreateOrderPayload, Order, PageResult, Result } from '@/types'

const enc = (orderNo: string) => encodeURIComponent(orderNo)

/** 订单服务接口（经 /api → 网关转发） */
export const orderApi = {
  /** 普通商品下单，价格由服务端核算，不传金额 */
  create: (data: CreateOrderPayload) => http.post<unknown, Result<Order>>('/orders', data),

  /** 我的订单（分页），status 不传查全部：0 待支付 / 1 已支付 / 2 已取消 */
  my: (params: { status?: number; page?: number; size?: number } = {}) =>
    http.get<unknown, Result<PageResult<Order>>>('/orders/my', { params }),

  /** 订单详情（本人或管理员） */
  detail: (orderNo: string) => http.get<unknown, Result<Order>>(`/orders/${enc(orderNo)}`),

  pay: (orderNo: string) => http.post<unknown, Result<null>>(`/orders/${enc(orderNo)}/pay`),

  /** 取消待支付订单，成功后服务端回补库存 */
  cancel: (orderNo: string) => http.post<unknown, Result<null>>(`/orders/${enc(orderNo)}/cancel`),

  /** 管理员：条件分页查询订单 */
  page: (params: { orderNo?: string; userId?: number; status?: number; page?: number; size?: number }) =>
    http.get<unknown, Result<PageResult<Order>>>('/orders/page', { params }),
}
