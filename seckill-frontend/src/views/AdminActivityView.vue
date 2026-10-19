<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { activityApi } from '@/api/seckill'
import { productApi } from '@/api/product'
import PagerBar from '@/components/PagerBar.vue'
import type { Product, SeckillActivity } from '@/types'
import {
  activityPhase,
  activityPhaseText,
  formatPrice,
  formatTime,
  toDateTimeInput,
} from '@/utils/format'
import { useToast } from '@/utils/toast'

const toast = useToast()

const activities = ref<SeckillActivity[]>([])
const loading = ref(true)
const errorMsg = ref('')
const products = ref<Product[]>([])

const activeTab = ref<'all' | 1 | 0>('all')
const page = ref(1)
const size = 10
const total = ref(0)

/** 当前正在编辑的活动 ID，null 表示新建 */
const editingId = ref<number | null>(null)
const showForm = ref(false)
const saving = ref(false)
const formError = ref('')
const busyId = ref<number | null>(null)

const form = reactive({
  productId: null as number | null,
  seckillPrice: null as number | null,
  stock: null as number | null,
  startTime: '',
  endTime: '',
})

const TABS = [
  { key: 'all', label: '全部活动' },
  { key: 1, label: '已上线' },
  { key: 0, label: '已下线' },
] as const

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await activityApi.page({
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      page: page.value,
      size,
    })
    activities.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/** 商品下拉：只取上架商品，秒杀必须挂在在售商品上 */
async function loadProducts() {
  try {
    const res = await productApi.page({ status: 1, page: 1, size: 200 })
    products.value = res.data?.records ?? []
  } catch {
    /* 商品加载失败不阻断活动管理，创建时可手动填 ID */
  }
}

function openCreate() {
  editingId.value = null
  form.productId = null
  form.seckillPrice = null
  form.stock = null
  form.startTime = ''
  form.endTime = ''
  formError.value = ''
  showForm.value = true
}

function openEdit(a: SeckillActivity) {
  editingId.value = a.id
  form.productId = a.productId
  form.seckillPrice = a.seckillPrice
  form.stock = a.stock
  form.startTime = toDateTimeInput(a.startTime)
  form.endTime = toDateTimeInput(a.endTime)
  formError.value = ''
  showForm.value = true
}

function validate(): boolean {
  formError.value = ''
  if (editingId.value === null && !form.productId) {
    formError.value = '请选择关联商品'
    return false
  }
  if (form.seckillPrice === null || form.seckillPrice < 0.01) {
    formError.value = '秒杀价不能低于 0.01 元'
    return false
  }
  if (form.stock === null || !Number.isInteger(form.stock) || form.stock < 1) {
    formError.value = '秒杀库存需为不小于 1 的整数'
    return false
  }
  if (!form.startTime || !form.endTime) {
    formError.value = '请填写活动的开始和结束时间'
    return false
  }
  const start = new Date(form.startTime).getTime()
  const end = new Date(form.endTime).getTime()
  if (!(start < end)) {
    formError.value = '开始时间必须早于结束时间'
    return false
  }
  if (editingId.value === null && start <= Date.now()) {
    formError.value = '新建活动的开始时间必须晚于当前时间'
    return false
  }
  return true
}

async function save() {
  if (!validate()) return
  const id = editingId.value
  saving.value = true
  try {
    if (id === null) {
      await activityApi.create({
        productId: form.productId as number,
        seckillPrice: form.seckillPrice as number,
        stock: form.stock as number,
        startTime: form.startTime,
        endTime: form.endTime,
      })
      toast.show('活动创建成功', 'success')
    } else {
      await activityApi.update(id, {
        seckillPrice: form.seckillPrice as number,
        stock: form.stock as number,
        startTime: form.startTime,
        endTime: form.endTime,
      })
      toast.show('活动已更新', 'success')
    }
    showForm.value = false
    page.value = 1
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败，请稍后再试'
  } finally {
    saving.value = false
  }
}

async function toggleOnline(a: SeckillActivity) {
  if (busyId.value !== null) return
  const toOnline = a.status !== 1
  if (
    !toOnline &&
    !window.confirm(`确定下线活动 #${a.id} 吗？下线后缓存会立即作废，用户无法再抢购。`)
  ) {
    return
  }
  busyId.value = a.id
  try {
    if (toOnline) {
      await activityApi.online(a.id)
      toast.show('活动已上线', 'success')
    } else {
      await activityApi.offline(a.id)
      toast.show('活动已下线', 'success')
    }
    await load()
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '操作失败，请稍后再试', 'error')
  } finally {
    busyId.value = null
  }
}

async function preload(a: SeckillActivity) {
  if (busyId.value !== null) return
  busyId.value = a.id
  try {
    await activityApi.preload(a.id)
    toast.show('预热成功，缓存已就绪', 'success')
    await load()
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '预热失败，请稍后再试', 'error')
  } finally {
    busyId.value = null
  }
}

function phaseOf(a: SeckillActivity): 0 | 1 | 2 {
  return activityPhase(a.startTime, a.endTime)
}

function phaseClass(phase: 0 | 1 | 2): string {
  return phase === 1 ? 'on' : phase === 0 ? 'wait' : 'muted'
}

function switchTab(key: 'all' | 1 | 0) {
  if (activeTab.value === key) return
  activeTab.value = key
  page.value = 1
  load()
}

function switchPage(p: number) {
  page.value = p
  load()
}

onMounted(() => {
  load()
  loadProducts()
})
</script>

<template>
  <div class="orders-page">
    <div class="container">
      <header class="page-head">
        <p class="hero-kicker">SECKILL ACTIVITY</p>
        <h1 class="hero-title">秒杀活动管理</h1>
        <p class="hero-sub">创建活动、上下线、预热缓存；活动开始前可修改秒杀价与库存</p>
      </header>

      <!-- 新建 / 编辑表单 -->
      <section v-if="showForm" class="seckill-panel form-block">
        <h2 class="panel-title">{{ editingId === null ? '新建活动' : `编辑活动 #${editingId}` }}</h2>

        <div class="field-row">
          <div class="field">
            <label class="field-label" for="ac-product">
              <span><i class="req">*</i>关联商品</span>
            </label>
            <select
              id="ac-product"
              v-model="form.productId"
              class="input"
              :disabled="editingId !== null"
            >
              <option :value="null">请选择商品</option>
              <option v-for="p in products" :key="p.id" :value="p.id">
                NO.{{ String(p.id).padStart(6, '0') }} · {{ p.name }}（¥{{ formatPrice(p.price) }}）
              </option>
            </select>
            <p v-if="editingId !== null" class="field-hint">活动创建后不允许更换商品</p>
          </div>
          <div class="field">
            <label class="field-label" for="ac-price"><span><i class="req">*</i>秒杀价（元）</span></label>
            <input
              id="ac-price"
              v-model.number="form.seckillPrice"
              class="input"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="不能高于商品原价"
            />
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label class="field-label" for="ac-stock"><span><i class="req">*</i>秒杀库存（件）</span></label>
            <input
              id="ac-stock"
              v-model.number="form.stock"
              class="input"
              type="number"
              min="1"
              step="1"
            />
          </div>
          <div class="field">
            <label class="field-label" for="ac-start"><span><i class="req">*</i>开始时间</span></label>
            <input id="ac-start" v-model="form.startTime" class="input" type="datetime-local" />
          </div>
          <div class="field">
            <label class="field-label" for="ac-end"><span><i class="req">*</i>结束时间</span></label>
            <input id="ac-end" v-model="form.endTime" class="input" type="datetime-local" />
          </div>
        </div>

        <p v-if="formError" class="field-error">{{ formError }}</p>

        <div class="form-actions">
          <button class="btn btn-primary btn-sm" type="button" :disabled="saving" @click="save">
            <span v-if="saving" class="spinner"></span>
            {{ saving ? '保存中' : editingId === null ? '创建活动' : '保存修改' }}
          </button>
          <button class="btn btn-ghost btn-sm" type="button" :disabled="saving" @click="showForm = false">
            取消
          </button>
        </div>
      </section>

      <!-- 列表 -->
      <section class="seckill-panel">
        <div class="panel-head-row">
          <h2 class="panel-title">活动列表</h2>
          <button v-if="!showForm" class="btn btn-primary btn-sm" type="button" @click="openCreate">
            新建活动
          </button>
        </div>

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

        <div v-else-if="activities.length === 0" class="state-block">
          <div class="state-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 2" />
            </svg>
          </div>
          <p class="state-title">暂无活动</p>
          <p class="state-desc">点击「新建活动」创建第一场秒杀</p>
        </div>

        <template v-else>
          <ul class="act-list">
            <li v-for="a in activities" :key="a.id" class="act-row">
              <router-link :to="`/products/${a.productId}`" class="act-thumb">
                <img v-if="a.productImage" :src="a.productImage" :alt="a.productName ?? ''" />
                <span v-else class="p-fallback"><span>秒</span></span>
              </router-link>

              <div class="act-info">
                <div class="act-name">
                  <strong>#{{ a.id }}</strong>
                  <router-link :to="`/products/${a.productId}`">
                    {{ a.productName || `商品 NO.${String(a.productId).padStart(6, '0')}` }}
                  </router-link>
                  <span class="act-phase" :class="phaseClass(phaseOf(a))">
                    {{ activityPhaseText(phaseOf(a)) }}
                  </span>
                  <span class="act-phase" :class="a.status === 1 ? 'on' : 'muted'">
                    {{ a.status === 1 ? '已上线' : '已下线' }}
                  </span>
                </div>
                <div class="act-price">
                  <span class="price"><i>¥</i>{{ formatPrice(a.seckillPrice) }}</span>
                  <span v-if="a.originalPrice" class="act-origin">原价 ¥{{ formatPrice(a.originalPrice) }}</span>
                  <span class="act-origin">剩余 {{ a.remainingStock }} / {{ a.stock }} 件</span>
                </div>
                <p class="act-meta">{{ formatTime(a.startTime) }} ~ {{ formatTime(a.endTime) }}</p>
              </div>

              <div class="act-btns">
                <button
                  v-if="phaseOf(a) === 0"
                  class="btn btn-line btn-sm"
                  type="button"
                  @click="openEdit(a)"
                >
                  编辑
                </button>
                <button
                  class="btn btn-line btn-sm"
                  type="button"
                  :disabled="busyId !== null"
                  @click="preload(a)"
                >
                  预热
                </button>
                <button
                  class="btn btn-sm"
                  :class="a.status === 1 ? 'btn-danger-line' : 'btn-primary'"
                  type="button"
                  :disabled="busyId !== null"
                  @click="toggleOnline(a)"
                >
                  {{ a.status === 1 ? '下线' : '上线' }}
                </button>
              </div>
            </li>
          </ul>

          <PagerBar :page="page" :size="size" :total="total" @change="switchPage" />
        </template>
      </section>
    </div>
  </div>
</template>
