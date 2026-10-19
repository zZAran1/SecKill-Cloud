<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { orderApi } from '@/api/order'
import PagerBar from '@/components/PagerBar.vue'
import type { Order } from '@/types'
import { formatPrice, formatTime, orderStatusText } from '@/utils/format'
import { useToast } from '@/utils/toast'

const toast = useToast()

const orders = ref<Order[]>([])
const loading = ref(true)
const errorMsg = ref('')
const payingNo = ref('')
const cancellingNo = ref('')

const activeTab = ref<'all' | 0 | 1 | 2>('all')
const page = ref(1)
const size = 10
const total = ref(0)

const TABS = [
  { key: 'all', label: '全部订单' },
  { key: 0, label: '待支付' },
  { key: 1, label: '已支付' },
  { key: 2, label: '已取消' },
] as const

function statusClass(status: number): string {
  return status === 1 ? 'on' : status === 2 ? 'muted' : 'off'
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await orderApi.my({
      status: activeTab.value === 'all' ? undefined : activeTab.value,
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

function switchTab(key: 'all' | 0 | 1 | 2) {
  if (activeTab.value === key) return
  activeTab.value = key
  page.value = 1
  load()
}

function switchPage(p: number) {
  page.value = p
  load()
}

async function pay(order: Order) {
  if (payingNo.value) return
  payingNo.value = order.orderNo
  try {
    await orderApi.pay(order.orderNo)
    toast.show('支付成功', 'success')
    await load()
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '支付失败，请稍后再试', 'error')
  } finally {
    payingNo.value = ''
  }
}

async function cancel(order: Order) {
  if (cancellingNo.value) return
  if (!window.confirm(`确定取消订单 ${order.orderNo} 吗？取消后库存将退回。`)) return
  cancellingNo.value = order.orderNo
  try {
    await orderApi.cancel(order.orderNo)
    toast.show('订单已取消', 'success')
    await load()
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '取消失败，请稍后再试', 'error')
  } finally {
    cancellingNo.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="orders-page">
    <div class="container">
      <header class="page-head">
        <p class="hero-kicker">MY ORDERS</p>
        <h1 class="hero-title">我的订单</h1>
        <p class="hero-sub">查看订单状态，及时完成支付；超时未支付的订单会被自动关闭</p>
      </header>

      <div v-if="!loading && !errorMsg" class="order-tabs">
        <button
          v-for="tab in TABS"
          :key="tab.key"
          class="order-tab"
          :class="{ on: activeTab === tab.key }"
          type="button"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="order-list">
        <div v-for="i in 3" :key="i" class="skel-card order-skel">
          <div class="skel skel-line" style="width: 30%"></div>
          <div class="skel skel-line" style="width: 60%"></div>
          <div class="skel skel-line short"></div>
        </div>
      </div>

      <!-- 加载失败 -->
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

      <!-- 空态 -->
      <div v-else-if="orders.length === 0" class="state-block">
        <div class="state-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 7h12l1.2 13H4.8L6 7Z" />
            <path d="M9 10V6a3 3 0 0 1 6 0v4" />
          </svg>
        </div>
        <p class="state-title">{{ activeTab === 'all' ? '还没有订单' : '该状态下暂无订单' }}</p>
        <p class="state-desc">去逛逛，把心仪好物带回家</p>
        <router-link :to="{ name: 'products' }" class="btn btn-line">去逛逛</router-link>
      </div>

      <!-- 订单列表 -->
      <template v-else>
        <div class="order-list">
          <article v-for="o in orders" :key="o.orderNo" class="order-card">
            <div class="order-top">
              <span class="order-no">订单号 {{ o.orderNo }}</span>
              <span class="order-status" :class="statusClass(o.status)">
                {{ o.statusDesc || orderStatusText(o.status) }}
              </span>
            </div>

            <div class="order-mid">
              <router-link :to="`/products/${o.productId}`" class="order-product">
                <span class="order-pid">
                  {{ o.activityId ? `秒杀商品 NO.${String(o.productId).padStart(6, '0')}` : `商品编号 NO.${String(o.productId).padStart(6, '0')}` }}
                </span>
                <span class="order-qty">共 {{ o.quantity }} 件</span>
              </router-link>
              <div class="order-amount">
                <span class="order-amount-label">实付</span>
                <span class="price"><i>¥</i>{{ formatPrice(o.totalAmount) }}</span>
              </div>
            </div>

            <div class="order-foot">
              <div class="order-meta">
                <span class="order-time">下单时间 {{ formatTime(o.createTime) }}</span>
                <span v-if="o.status === 0 && o.expireTime" class="order-deadline">
                  请在 {{ formatTime(o.expireTime) }} 前完成支付
                </span>
              </div>
              <div class="order-btns">
                <button
                  v-if="o.status === 0"
                  class="btn btn-danger-line btn-sm"
                  type="button"
                  :disabled="!!cancellingNo"
                  @click="cancel(o)"
                >
                  <span v-if="cancellingNo === o.orderNo" class="spinner"></span>
                  {{ cancellingNo === o.orderNo ? '取消中' : '取消订单' }}
                </button>
                <button
                  v-if="o.status === 0"
                  class="btn btn-primary btn-sm"
                  type="button"
                  :disabled="!!payingNo"
                  @click="pay(o)"
                >
                  <span v-if="payingNo === o.orderNo" class="spinner"></span>
                  {{ payingNo === o.orderNo ? '支付中' : '立即支付' }}
                </button>
                <router-link :to="`/products/${o.productId}`" class="btn btn-line btn-sm">查看商品</router-link>
              </div>
            </div>
          </article>
        </div>

        <PagerBar :page="page" :size="size" :total="total" @change="switchPage" />
      </template>
    </div>
  </div>
</template>
