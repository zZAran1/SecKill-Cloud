import http from './http'
import type {
  PageResult,
  Product,
  ProductPayload,
  ProductStatusPayload,
  Result,
  UpdateProductPayload,
} from '@/types'

/** 商品服务接口（8082，经 /api → 网关转发） */
export const productApi = {
  /** 全量列表（只返回上架商品） */
  list: (keyword?: string) =>
    http.get<unknown, Result<Product[]>>('/products', {
      params: keyword ? { keyword } : {},
    }),

  detail: (id: number | string) => http.get<unknown, Result<Product>>(`/products/${id}`),

  create: (data: ProductPayload) => http.post<unknown, Result<null>>('/products', data),

  /**
   * 分页查询。status 不传只返回上架商品，管理端传 0/1 查看下架或上架。
   */
  page: (params: { keyword?: string; status?: number; page?: number; size?: number }) =>
    http.get<unknown, Result<PageResult<Product>>>('/products/page', { params }),

  /** 编辑商品，字段可选 */
  update: (id: number, data: UpdateProductPayload) =>
    http.put<unknown, Result<null>>(`/products/${id}`, data),

  /** 上下架：status 1 上架 / 0 下架 */
  updateStatus: (id: number, data: ProductStatusPayload) =>
    http.put<unknown, Result<null>>(`/products/${id}/status`, data),
}
