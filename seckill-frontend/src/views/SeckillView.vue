<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { activityApi, seckillApi } from '@/api/seckill'
import { ApiError, type SeckillActivity, type SeckillResult } from '@/types'
import { useAuth } from '@/stores/auth'
import {
  activityPhase,
  activityPhaseText,
  formatPrice,
  formatTime,
} from '@/utils/format'
import { useToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const { isAdmin, isLoggedIn } = useAuth()
const toast = useToast()

const activities = ref<SeckillActivity[]>([])
const loading = ref(true)
const errorMsg = ref('')

/** 正在抢购的活动 ID */
const rushingId = ref<number | null>(null)
const preloadingId = ref<number | null>(null)

/** 抢购结果：null 表示还没开始抢 */
const result = ref<SeckillResult | null>(null)
const polling = ref(false)

let timer: number | undefined

/** 简单节流：两次提交至少间隔 1 秒，配合网关限流双保险 */
let lastSubmitAt = 0

const SECKILL_MSG: Record<number, string> = {
  4001: '秒杀活动不存在，请核对活动编号',
  4002: '活动尚未开始，请准时再来',
  4003: '本场活动已经结束',
  4004: '手慢了，本场库存已抢完',
  4005: '你已参与过本场活动，请勿重复抢购',
}

function messageOf(e: unknown, fallback: string): string {
  if (e instanceof ApiError) return SECKILL_MSG[e.code] || e.message || fallback
  return e instanceof Error ? e.message : fallback
}

function phaseOf(a: SeckillActivity): 0 | 1 | 2 {
  return activityPhase(a.startTime, a.endTime)
}

function phaseClass(phase: 0 | 1 | 2): string {
  return phase === 1 ? 'on' : phase === 0 ? 'wait' : 'muted'
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await activityApi.page({ page: 1, size: 50 })
    activities.value = res.data?.records ?? []
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/** 抢购后轮询结果，直到出结果或超过 30 秒 */
async function pollResult(activityId: number, attempts = 0) {
  try {
    const res = await seckillApi.result(activityId)
    const r = res.data
    result.value = r
    if (r.result === 0 && attempts < 20) {
      polling.value = true
      timer = window.setTimeout(() => pollResult(activityId, attempts + 1), 1500)
    } else {
      polling.value = false
    }
  } catch {
    polling.value = false
  }
}

async function rush(a: SeckillActivity) {
  if (rushingId.value !== null) return
  // 活动列表是公开的，抢购需要登录
  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const now = Date.now()
  if (now - lastSubmitAt < 1000) return
  lastSubmitAt = now

  rushingId.value = a.id
  result.value = null
  try {
    await seckillApi.rush(a.id)
    result.value = { result: 0, message: '抢购请求已受理，排队建单中…', orderNo: null, orderStatus: null }
    pollResult(a.id)
  } catch (e) {
    polling.value = false
    result.value = { result: 2, message: messageOf(e, '抢购失败，请稍后再试'), orderNo: null, orderStatus: null }
  } finally {
    rushingId.value = null
    load()
  }
}

async function preload(a: SeckillActivity) {
  if (preloadingId.value !== null) return
  preloadingId.value = a.id
  try {
    await activityApi.preload(a.id)
    toast.show('预热成功，缓存已就绪', 'success')
    await load()
  } catch (e) {
    toast.show(messageOf(e, '预热失败，请稍后再试'), 'error')
  } finally {
    preloadingId.value = null
  }
}

onMounted(load)

onBeforeUnmount(() => {
  if (timer) window.clearTimeout(timer)
})
</script>

<template>
  <div class="seckill-page">
    <div class="container">
      <header class="page-head">
        <p class="hero-kicker">SECKILL NOW</p>
        <h1 class="hero-title">秒杀专场</h1>
        <p class="hero-sub">每场限购一次，先到先得；抢购后由系统异步建单</p>
      </header>

      <div class="seckill-grid">
        <section class="seckill-panel">
          <h2 class="panel-title">进行中的活动</h2>

          <!-- 加载中 -->
          <div v-if="loading" class="order-list">
            <div v-for="i in 3" :key="i" class="skel-card order-skel">
              <div class="skel skel-line" style="width: 40%"></div>
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
          <div v-else-if="activities.length === 0" class="state-block">
            <div class="state-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="9" />
                <path d="M12 7v5l3 2" />
              </svg>
            </div>
            <p class="state-title">暂无秒杀活动</p>
            <p class="state-desc">活动上线后会展示在这里</p>
          </div>

          <!-- 活动列表 -->
          <ul v-else class="act-list">
            <li v-for="a in activities" :key="a.id" class="act-row">
              <router-link :to="`/products/${a.productId}`" class="act-thumb">
                <img v-if="a.productImage" :src="a.productImage" :alt="a.productName ?? ''" />
                <span v-else class="p-fallback"><span>秒</span></span>
              </router-link>

              <div class="act-info">
                <div class="act-name">
                  <router-link :to="`/products/${a.productId}`">
                    {{ a.productName || `商品 NO.${String(a.productId).padStart(6, '0')}` }}
                  </router-link>
                  <span class="act-phase" :class="phaseClass(phaseOf(a))">
                    {{ activityPhaseText(phaseOf(a)) }}
                  </span>
                </div>
                <div class="act-price">
                  <span class="price"><i>¥</i>{{ formatPrice(a.seckillPrice) }}</span>
                  <span v-if="a.originalPrice" class="act-origin">原价 ¥{{ formatPrice(a.originalPrice) }}</span>
                </div>
                <p class="act-meta">
                  {{ formatTime(a.startTime) }} ~ {{ formatTime(a.endTime) }} · 剩余
                  <strong>{{ a.remainingStock }}</strong> 件
                </p>
              </div>

              <div class="act-btns">
                <button
                  v-if="isAdmin"
                  class="btn btn-line btn-sm"
                  type="button"
                  :disabled="preloadingId !== null"
                  @click="preload(a)"
                >
                  <span v-if="preloadingId === a.id" class="spinner"></span>
                  {{ preloadingId === a.id ? '预热中' : '预热' }}
                </button>
                <button
                  class="btn btn-primary btn-sm"
                  type="button"
                  :disabled="phaseOf(a) !== 1 || rushingId !== null || a.remainingStock <= 0"
                  @click="rush(a)"
                >
                  <span v-if="rushingId === a.id" class="spinner"></span>
                  {{ rushingId === a.id ? '抢购中' : a.remainingStock <= 0 ? '已抢完' : '立即抢购' }}
                </button>
              </div>
            </li>
          </ul>

          <!-- 抢购结果 -->
          <div
            v-if="result"
            class="form-alert"
            :class="{ 'alert-ok': result.result === 1 }"
          >
            <template v-if="result.result === 0">
              {{ result.message }}
              <span v-if="polling" class="spinner spinner-inline"></span>
            </template>
            <template v-else-if="result.result === 1">
              {{ result.message }}
              <router-link :to="{ name: 'orders' }" class="alert-link">去支付</router-link>
            </template>
            <template v-else>{{ result.message }}</template>
          </div>

          <p class="seckill-note">
            抢购成功后由系统异步生成订单，每位用户每场限购一次；同一活动重复抢购会被拒绝。
            下单后 15 分钟内未支付将自动关闭并回补库存。
          </p>
        </section>

        <aside class="seckill-guide">
          <h3 class="panel-title">抢购流程</h3>
          <ol class="steps">
            <li>
              <i>1</i>
              <div>
                <strong>活动预热</strong>
                <p>开抢前由管理员把活动库存加载进缓存</p>
              </div>
            </li>
            <li>
              <i>2</i>
              <div>
                <strong>准时抢购</strong>
                <p>活动开始后点击「立即抢购」，先到先得</p>
              </div>
            </li>
            <li>
              <i>3</i>
              <div>
                <strong>排队建单</strong>
                <p>抢购成功后系统异步为你生成秒杀订单</p>
              </div>
            </li>
            <li>
              <i>4</i>
              <div>
                <strong>完成支付</strong>
                <p>到「我的订单」完成支付，即抢购成功</p>
              </div>
            </li>
          </ol>
        </aside>
      </div>
    </div>
  </div>
</template>
