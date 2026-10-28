<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { orderApi } from '@/api/order'
import PagerBar from '@/components/PagerBar.vue'
import type { Order } from '@/types'
import { formatPrice, formatTime, orderStatusText } from '@/utils/format'
import { useToast } from '@/utils/toast'

const toast = useToast()

const orders = ref<Order[]>([])
const loading = ref(true)
const errorMsg = ref('')
const busyNo = ref('')

const query = reactive({
  orderNo: '',
  userId: '' as string | number,
  status: '' as string | number,
})
const page = ref(1)
const size = 10
const total = ref(0)

function statusClass(status: number): string {
  return status === 1 || status === 3 ? 'on' : status === 2 || status === 5 ? 'muted' : 'off'
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await orderApi.page({
      orderNo: query.orderNo.trim() || undefined,
      userId: query.userId === '' ? undefined : Number(query.userId),
      status: query.status === '' ? undefined : Number(query.status),
      page: page.value,
      size,
    })
    orders.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  query.orderNo = ''
  query.userId = ''
  query.status = ''
  page.value = 1
  load()
}

function switchPage(p: number) {
  page.value = p
  load()
}

/** 管理员代关闭：只允许关闭待支付订单，服务端会同步回补库存 */
async function closeOrder(o: Order) {
  if (busyNo.value) return
  if (!window.confirm(`确定关闭订单 ${o.orderNo} 吗？关闭后库存将退回。`)) return
  busyNo.value = o.orderNo
  try {
    await orderApi.cancel(o.orderNo)
    toast.show('订单已关闭', 'success')
    await load()
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '操作失败，请稍后再试', 'error')
  } finally {
    busyNo.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="orders-page">
    <div class="container">
      <header class="page-head">
        <p class="hero-kicker">ORDER ADMIN</p>
        <h1 class="hero-title">订单管理</h1>
        <p class="hero-sub">按订单号、用户或状态检索；可代关闭未支付的订单</p>
      </header>

      <section class="seckill-panel">
        <div class="search-bar search-bar-wide">
          <div class="search-box">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.2-3.2" />
            </svg>
            <input
              v-model="query.orderNo"
              class="input"
              placeholder="按订单号搜索"
              @keyup.enter="search"
            />
          </div>
          <input
            v-model="query.userId"
            class="input query-input"
            inputmode="numeric"
            placeholder="用户 ID"
            @keyup.enter="search"
          />
          <select v-model="query.status" class="input query-select">
            <option value="">全部状态</option>
            <option :value="0">待支付</option>
            <option :value="1">已支付</option>
            <option :value="2">已取消</option>
            <option :value="3">已完成</option>
            <option :value="4">退款中</option>
            <option :value="5">已退款</option>
          </select>
          <button class="btn btn-primary" type="button" @click="search">搜索</button>
          <button class="btn btn-line" type="button" @click="reset">重置</button>
        </div>

        <div v-if="loading" class="order-list">
          <div v-for="i in 3" :key="i" class="skel-card order-skel">
            <div class="skel skel-line" style="width: 40%"></div>
            <div class="skel skel-line short"></div>
          </div>
        </div>

        <div v-else-if="errorMsg" class="state-block">
          <div class="state-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 8v5M12 16.5h.01" />
            </svg>
          </div>
          <p class="state-title">加载失败</p>
          <p class="state-desc">{{ errorMsg }}</p>
          <button class="btn btn-line" type="button" @click="load">重新加载</button>
        </div>

        <div v-else-if="orders.length === 0" class="state-block">
          <div class="state-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 7h12l1.2 13H4.8L6 7Z" />
              <path d="M9 10V6a3 3 0 0 1 6 0v4" />
            </svg>
          </div>
          <p class="state-title">没有匹配的订单</p>
          <p class="state-desc">换个条件再试试</p>
        </div>

        <template v-else>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>用户 ID</th>
                  <th>商品 ID</th>
                  <th>类型</th>
                  <th>数量</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>下单时间</th>
                  <th>支付截止</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in orders" :key="o.orderNo">
                  <td class="num">{{ o.orderNo }}</td>
                  <td class="num">{{ o.userId }}</td>
                  <td class="num">
                    <router-link :to="`/products/${o.productId}`" class="table-link">
                      {{ o.productId }}
                    </router-link>
                  </td>
                  <td>{{ o.activityId ? `秒杀 #${o.activityId}` : '普通' }}</td>
                  <td class="num">{{ o.quantity }}</td>
                  <td class="num">¥{{ formatPrice(o.totalAmount) }}</td>
                  <td>
                    <span class="order-status" :class="statusClass(o.status)">
                      {{ o.statusDesc || orderStatusText(o.status) }}
                    </span>
                  </td>
                  <td class="num">{{ formatTime(o.createTime) }}</td>
                  <td class="num">{{ o.status === 0 ? formatTime(o.expireTime) : '--' }}</td>
                  <td>
                    <button
                      v-if="o.status === 0"
                      class="btn btn-danger-line btn-sm"
                      type="button"
                      :disabled="!!busyNo"
                      @click="closeOrder(o)"
                    >
                      <span v-if="busyNo === o.orderNo" class="spinner"></span>
                      {{ busyNo === o.orderNo ? '关闭中' : '关闭订单' }}
                    </button>
                    <span v-else class="table-dash">--</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <PagerBar :page="page" :size="size" :total="total" @change="switchPage" />
        </template>
      </section>
    </div>
  </div>
</template>
