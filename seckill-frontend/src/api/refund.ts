import http from './http'
import type { CreateRefundPayload, PageResult, Refund, Result } from '@/types'

const enc = (refundNo: string) => encodeURIComponent(refundNo)

/** 退款服务接口（经 /api → 网关转发） */
export const refundApi = {
  /** 提交退款申请（订单进入退款中，待审核） */
  apply: (data: CreateRefundPayload) => http.post<unknown, Result<Refund>>('/refunds', data),

  /** 撤回待审核的退款申请，订单恢复已支付 */
  cancelByOrder: (orderNo: string) =>
    http.post<unknown, Result<null>>(`/refunds/orders/${enc(orderNo)}/cancel`),

  /** 我的退款单（分页），status 不传查全部 */
  my: (params: { status?: number; page?: number; size?: number } = {}) =>
    http.get<unknown, Result<PageResult<Refund>>>('/refunds/my', { params }),

  /** 管理员：条件分页查询退款单 */
  page: (params: { orderNo?: string; userId?: number; status?: number; page?: number; size?: number }) =>
    http.get<unknown, Result<PageResult<Refund>>>('/refunds/page', { params }),

  /** 管理员审核通过：渠道退款并把订单转为已退款 */
  approve: (refundNo: string) => http.post<unknown, Result<Refund>>(`/refunds/${enc(refundNo)}/approve`),

  /** 管理员拒绝，订单恢复已支付 */
  reject: (refundNo: string, remark?: string) =>
    http.post<unknown, Result<Refund>>(`/refunds/${enc(refundNo)}/reject`, undefined, {
      params: remark ? { remark } : undefined,
    }),
}
