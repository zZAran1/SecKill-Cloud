<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '@/api/product'
import type { Product } from '@/types'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()

const products = ref<Product[]>([])
const loading = ref(true)
const errorMsg = ref('')
const keyword = ref((route.query.keyword as string) || '')
const applied = ref(keyword.value)

async function load(kw = '') {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await productApi.list(kw || undefined)
    products.value = res.data ?? []
    applied.value = kw
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

function search() {
  const kw = keyword.value.trim()
  router.replace({ query: kw ? { keyword: kw } : {} })
  load(kw)
}

function clearSearch() {
  keyword.value = ''
  router.replace({ query: {} })
  load('')
}

onMounted(() => load(applied.value))
</script>

<template>
  <div>
    <section class="list-hero">
      <div class="container">
        <p class="hero-kicker">SECKILL COLLECTION</p>
        <h1 class="hero-title">全部商品</h1>
        <p class="hero-sub">每一件，都值得准时守候</p>
        <span class="hero-deco">匠心臻选</span>

        <form class="search-bar" @submit.prevent="search">
          <div class="search-box">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.8-3.8" />
            </svg>
            <input
              v-model="keyword"
              class="input"
              maxlength="50"
              placeholder="搜索商品名称"
              @keyup.enter="search"
            />
          </div>
          <button class="btn btn-primary" type="submit">搜索</button>
        </form>
      </div>
    </section>

    <section class="list-body">
      <div class="container">
        <div class="list-meta">
          <p class="list-count">
            <template v-if="applied">「{{ applied }}」的搜索结果，</template>
            共 <strong>{{ loading ? '…' : products.length }}</strong> 件在售商品
          </p>
          <button v-if="applied && !loading" class="list-clear" type="button" @click="clearSearch">清除搜索</button>
        </div>

        <!-- 加载中：骨架屏 -->
        <div v-if="loading" class="p-grid">
          <div v-for="i in 8" :key="i" class="skel-card">
            <div class="skel skel-media"></div>
            <div class="skel skel-line"></div>
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
          <button class="btn btn-line" type="button" @click="load(applied)">重新加载</button>
        </div>

        <!-- 空态 -->
        <div v-else-if="products.length === 0" class="state-block">
          <div class="state-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 7h12l1.2 13H4.8L6 7Z" />
              <path d="M9 10V6a3 3 0 0 1 6 0v4" />
            </svg>
          </div>
          <p class="state-title">{{ applied ? '未找到相关商品' : '暂无在售商品' }}</p>
          <p class="state-desc">{{ applied ? '换个关键词试试' : '敬请期待下一场' }}</p>
          <button v-if="applied" class="btn btn-line" type="button" @click="clearSearch">查看全部商品</button>
        </div>

        <!-- 商品网格 -->
        <div v-else class="p-grid">
          <ProductCard
            v-for="(p, i) in products"
            :key="p.id"
            :product="p"
            :style="{ animationDelay: `${Math.min(i, 12) * 45}ms` }"
          />
        </div>
      </div>
    </section>
  </div>
</template>
