<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '@/api/product'
import { orderApi } from '@/api/order'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { ApiError, type Product } from '@/types'
import { formatPrice, formatTime } from '@/utils/format'
import { safeImageUrl } from '@/utils/security'

const route = useRoute()
const router = useRouter()
const { isLoggedIn, isAdmin } = useAuth()
const toast = useToast()

const product = ref<Product | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const notFound = ref(false)
const imgFailed = ref(false)

const quantity = ref(1)
const buying = ref(false)
const toggling = ref(false)

/** 图片地址经协议白名单过滤，防 javascript: 等脚本注入 */
const safeImage = computed(() => safeImageUrl(product.value?.imageUrl))

const canBuy = computed(
  () => !!product.value && product.value.status === 1 && product.value.stock > 0 && !buying.value,
)

async function load() {
  loading.value = true
  errorMsg.value = ''
  notFound.value = false
  try {
    const res = await productApi.detail(route.params.id as string)
    product.value = res.data
  } catch (e) {
    if (e instanceof ApiError && e.code === 2001) {
      notFound.value = true
    } else {
      errorMsg.value = e instanceof Error ? e.message : '加载失败，请稍后再试'
    }
  } finally {
    loading.value = false
  }
}

function step(delta: number) {
  const max = product.value?.stock ?? 1
  quantity.value = Math.min(Math.max(quantity.value + delta, 1), Math.max(max, 1))
}

async function buy() {
  const p = product.value
  if (!p || !canBuy.value) return

  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  const qty = Math.min(Math.max(Math.floor(quantity.value) || 1, 1), Math.max(p.stock, 1))
  quantity.value = qty

  buying.value = true
  try {
    await orderApi.create({ productId: p.id, quantity: qty })
    toast.show('下单成功，请尽快完成支付', 'success')
    router.push({ name: 'orders' })
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '下单失败，请稍后再试', 'error')
  } finally {
    buying.value = false
  }
}

/** 管理员：上下架切换 */
async function toggleStatus() {
  const p = product.value
  if (!p || toggling.value) return
  const next = p.status === 1 ? 0 : 1
  if (!window.confirm(next === 0 ? `确定下架「${p.name}」吗？` : `确定重新上架「${p.name}」吗？`)) {
    return
  }
  toggling.value = true
  try {
    await productApi.updateStatus(p.id, { status: next })
    p.status = next
    toast.show(next === 1 ? '商品已上架' : '商品已下架', 'success')
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '操作失败，请稍后再试', 'error')
  } finally {
    toggling.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="detail-page">
    <div class="container">
      <router-link :to="{ name: 'products' }" class="crumb">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 5l-7 7 7 7" />
        </svg>
        返回商品列表
      </router-link>

      <!-- 骨架屏 -->
      <div v-if="loading" class="detail-skel">
        <div class="skel skel-block"></div>
        <div>
          <div class="skel skel-line" style="width: 30%"></div>
          <div class="skel skel-line" style="width: 72%; height: 22px"></div>
          <div class="skel skel-line" style="width: 45%"></div>
          <div class="skel skel-line" style="width: 88%; margin-top: 42px"></div>
          <div class="skel skel-line" style="width: 88%"></div>
          <div class="skel skel-line" style="width: 60%"></div>
        </div>
      </div>

      <!-- 商品不存在 -->
      <div v-else-if="notFound" class="state-block">
        <div class="state-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 7h12l1.2 13H4.8L6 7Z" />
            <path d="M9 10V6a3 3 0 0 1 6 0v4" />
          </svg>
        </div>
        <p class="state-title">商品不存在或已下架</p>
        <p class="state-desc">它可能刚刚被抢完，去别处看看吧</p>
        <router-link :to="{ name: 'products' }" class="btn btn-line">返回商品列表</router-link>
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

      <!-- 商品内容 -->
      <div v-else-if="product" class="detail-grid">
        <div class="detail-media">
          <img
            v-if="safeImage && !imgFailed"
            :src="safeImage"
            :alt="product.name"
            @error="imgFailed = true"
          />
          <div v-else class="p-fallback">
            <span>{{ product.name.slice(0, 1) }}</span>
          </div>
        </div>

        <div class="detail-info">
          <span class="tag" :class="product.status === 1 ? 'tag-on' : 'tag-off'">
            {{ product.status === 1 ? '在售中' : '已下架' }}
          </span>
          <h1 class="detail-name">{{ product.name }}</h1>
          <p class="detail-id">商品编号 NO.{{ String(product.id).padStart(6, '0') }}</p>

          <div class="price-panel">
            <span class="price-label">售价</span>
            <span class="price-now"><i>¥</i>{{ formatPrice(product.price) }}</span>
            <span class="price-stock">库存 {{ product.stock }} 件</span>
          </div>

          <section class="buy-panel">
            <div class="qty-row">
              <span class="qty-label">购买数量</span>
              <div class="qty-stepper">
                <button type="button" :disabled="quantity <= 1" @click="step(-1)">−</button>
                <input
                  v-model.number="quantity"
                  type="number"
                  min="1"
                  :max="Math.max(product.stock, 1)"
                  @blur="quantity = Math.min(Math.max(Math.floor(quantity) || 1, 1), Math.max(product.stock, 1))"
                />
                <button type="button" :disabled="quantity >= product.stock" @click="step(1)">+</button>
              </div>
            </div>
            <div class="buy-actions">
              <button class="btn btn-primary" type="button" :disabled="!canBuy" @click="buy">
                <span v-if="buying" class="spinner"></span>
                {{ buying ? '下单中' : product.stock > 0 ? '立即购买' : '已售罄' }}
              </button>
              <router-link :to="{ name: 'seckill' }" class="btn btn-line">参与秒杀</router-link>
            </div>
            <p class="buy-note">下单后请到「我的订单」完成支付</p>
          </section>

          <section v-if="isAdmin" class="detail-sec">
            <h2>管理操作</h2>
            <div class="admin-actions">
              <router-link
                :to="{ name: 'product-edit', params: { id: product.id } }"
                class="btn btn-line btn-sm"
              >
                编辑商品
              </router-link>
              <button
                class="btn btn-sm"
                :class="product.status === 1 ? 'btn-danger-line' : 'btn-primary'"
                type="button"
                :disabled="toggling"
                @click="toggleStatus"
              >
                <span v-if="toggling" class="spinner"></span>
                {{ product.status === 1 ? '下架商品' : '重新上架' }}
              </button>
            </div>
          </section>

          <section class="detail-sec">
            <h2>商品介绍</h2>
            <p class="detail-desc">{{ product.description || '卖家很懒，什么也没有留下。' }}</p>
          </section>

          <section class="detail-sec">
            <h2>商品信息</h2>
            <dl class="meta-list">
              <div class="meta-item">
                <dt>上架时间</dt>
                <dd>{{ formatTime(product.createTime) }}</dd>
              </div>
              <div class="meta-item">
                <dt>最近更新</dt>
                <dd>{{ formatTime(product.updateTime) }}</dd>
              </div>
            </dl>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>
