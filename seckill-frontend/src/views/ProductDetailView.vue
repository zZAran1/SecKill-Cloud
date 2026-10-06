<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { productApi } from '@/api/product'
import { ApiError, type Product } from '@/types'
import { formatPrice, formatTime } from '@/utils/format'

const route = useRoute()

const product = ref<Product | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const notFound = ref(false)
const imgFailed = ref(false)

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
            v-if="product.imageUrl && !imgFailed"
            :src="product.imageUrl"
            :alt="product.name"
            @error="imgFailed = true"
          />
          <div v-else class="p-fallback">
            <span>{{ product.name.slice(0, 1) }}</span>
          </div>
        </div>

        <div class="detail-info">
          <span class="tag tag-on">在售中</span>
          <h1 class="detail-name">{{ product.name }}</h1>
          <p class="detail-id">商品编号 NO.{{ String(product.id).padStart(6, '0') }}</p>

          <div class="price-panel">
            <span class="price-label">秒杀价</span>
            <span class="price-now"><i>¥</i>{{ formatPrice(product.price) }}</span>
          </div>

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
