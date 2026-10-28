<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { refundApi } from '@/api/refund'
import PagerBar from '@/components/PagerBar.vue'
import type { Refund } from '@/types'
import { formatPrice, formatTime } from '@/utils/format'
import { useToast } from '@/utils/toast'

const toast = useToast()

const refunds = ref<Refund[]>([])
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
  return status === 1 ? 'on' : status === 2 ? 'muted' : 'off'
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await refundApi.page({
      orderNo: query.orderNo.trim() || undefined,
      userId: query.userId === '' ? undefined : Number(query.userId),
      status: query.status === '' ? undefined : Number(query.status),
      page: page.value,
      size,
    })
    refunds.value = res.data?.records ?? []
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

/** 审核通过：服务端调渠道退款、订单转已退款并回补库存 */
async function approve(r: Refund) {
  if (busyNo.value) return
  if (!window.confirm(`确定通过退款申请 ${r.refundNo} 吗？通过后将向用户退回 ¥${formatPrice(r.amount)}。`)) return
  busyNo.value = r.refundNo
  try {
    await refundApi.approve(r.refundNo)
    toast.show('退款成功', 'success')
    await load()
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '操作失败，请稍后再试', 'error')
  } finally {
    busyNo.value = ''
  }
}

/** 拒绝退款：订单恢复已支付 */
async function reject(r: Refund) {
  if (busyNo.value) return
  const remark = window.prompt(`拒绝退款申请 ${r.refundNo}\n请填写拒绝原因（可空）：`)
  if (remark === null) return
  busyNo.value = r.refundNo
  try {
    await refundApi.reject(r.refundNo, remark.trim() || undefined)
    toast.show('已拒绝该退款申请', 'success')
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
        <p class="hero-kicker">REFUND ADMIN</p>
        <h1 class="hero-title">退款审核</h1>
        <p class="hero-sub">审核用户的退款申请：通过后原路退款并回补库存，拒绝则订单恢复已支付</p>
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
            <option :value="0">待审核</option>
            <option :value="1">退款成功</option>
            <option :value="2">已拒绝</option>
            <option :value="3">已取消</option>
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

        <div v-else-if="refunds.length === 0" class="state-block">
          <div class="state-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 7h12l1.2 13H4.8L6 7Z" />
              <path d="M9 10V6a3 3 0 0 1 6 0v4" />
            </svg>
          </div>
          <p class="state-title">没有匹配的退款单</p>
          <p class="state-desc">换个条件再试试</p>
        </div>

        <template v-else>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>退款单号</th>
                  <th>订单号</th>
                  <th>用户 ID</th>
                  <th>金额</th>
                  <th>退款原因</th>
                  <th>状态</th>
                  <th>审核备注</th>
                  <th>申请时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in refunds" :key="r.refundNo">
                  <td class="num">{{ r.refundNo }}</td>
                  <td class="num">{{ r.orderNo }}</td>
                  <td class="num">{{ r.userId }}</td>
                  <td class="num">¥{{ formatPrice(r.amount) }}</td>
                  <td>{{ r.reason }}</td>
                  <td>
                    <span class="order-status" :class="statusClass(r.status)">
                      {{ r.statusDesc }}
                    </span>
                  </td>
                  <td>{{ r.auditRemark || '--' }}</td>
                  <td class="num">{{ formatTime(r.createTime) }}</td>
                  <td>
                    <template v-if="r.status === 0">
                      <button
                        class="btn btn-primary btn-sm"
                        type="button"
                        :disabled="!!busyNo"
                        @click="approve(r)"
                      >
                        <span v-if="busyNo === r.refundNo" class="spinner"></span>
                        {{ busyNo === r.refundNo ? '处理中' : '通过' }}
                      </button>
                      <button
                        class="btn btn-danger-line btn-sm"
                        type="button"
                        :disabled="!!busyNo"
                        @click="reject(r)"
                      >
                        拒绝
                      </button>
                    </template>
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
