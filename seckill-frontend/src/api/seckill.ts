import http from './http'
import type {
  ActivityPayload,
  PageResult,
  Result,
  SeckillActivity,
  SeckillResult,
  UpdateActivityPayload,
} from '@/types'

/** 秒杀下单与结果查询（订单服务），经 /api → 网关转发 */
export const seckillApi = {
  /** 参与秒杀，成功后异步排队建单 */
  rush: (activityId: number | string) =>
    http.post<unknown, Result<null>>(`/seckill/${encodeURIComponent(String(activityId))}`),

  /** 轮询秒杀结果：0 排队中 / 1 抢购成功 / 2 未抢到 */
  result: (activityId: number | string) =>
    http.get<unknown, Result<SeckillResult>>(
      `/seckill/${encodeURIComponent(String(activityId))}/result`,
    ),
}

/** 秒杀活动管理与浏览（商品服务），经 /api → 网关转发 */
export const activityApi = {
  /** 分页查询活动，status 不传只返回上线活动 */
  page: (params: { status?: number; page?: number; size?: number }) =>
    http.get<unknown, Result<PageResult<SeckillActivity>>>('/seckill-activities/page', {
      params,
    }),

  /** 活动详情（含实时剩余库存） */
  detail: (id: number | string) =>
    http.get<unknown, Result<SeckillActivity>>(`/seckill-activities/${id}`),

  create: (data: ActivityPayload) =>
    http.post<unknown, Result<SeckillActivity>>('/seckill-activities', data),

  /** 修改活动，仅未开始的活动可改 */
  update: (id: number, data: UpdateActivityPayload) =>
    http.put<unknown, Result<SeckillActivity>>(`/seckill-activities/${id}`, data),

  online: (id: number) => http.put<unknown, Result<null>>(`/seckill-activities/${id}/online`),

  offline: (id: number) => http.put<unknown, Result<null>>(`/seckill-activities/${id}/offline`),

  /** 管理员预热：把活动信息与库存加载进缓存 */
  preload: (id: number | string) =>
    http.post<unknown, Result<null>>(`/seckill-activities/${id}/preload`),
}
