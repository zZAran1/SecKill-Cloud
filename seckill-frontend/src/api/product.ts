import http from './http'
import type { Product, ProductPayload, Result } from '@/types'

/** 商品服务接口（8082，经 /api 代理） */
export const productApi = {
  /** 关键词可选；后端无分页，返回完整数组 */
  list: (keyword?: string) =>
    http.get<unknown, Result<Product[]>>('/products', {
      params: keyword ? { keyword } : {},
    }),

  detail: (id: number | string) => http.get<unknown, Result<Product>>(`/products/${id}`),

  create: (data: ProductPayload) => http.post<unknown, Result<null>>('/products', data),
}
