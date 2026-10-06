<script setup lang="ts">
import { ref } from 'vue'
import type { Product } from '@/types'
import { formatPrice } from '@/utils/format'

const props = defineProps<{ product: Product }>()

const imgFailed = ref(false)
</script>

<template>
  <router-link :to="`/products/${props.product.id}`" class="p-card">
    <div class="p-media">
      <img
        v-if="props.product.imageUrl && !imgFailed"
        :src="props.product.imageUrl"
        :alt="props.product.name"
        loading="lazy"
        @error="imgFailed = true"
      />
      <div v-else class="p-fallback">
        <span>{{ props.product.name.slice(0, 1) }}</span>
      </div>
      <span class="p-tag">在售</span>
    </div>
    <div class="p-body">
      <h3 class="p-name" :title="props.product.name">{{ props.product.name }}</h3>
      <p class="p-desc">{{ props.product.description || '暂无商品描述' }}</p>
      <div class="p-foot">
        <span class="price"><i>¥</i>{{ formatPrice(props.product.price) }}</span>
        <span class="p-arrow">→</span>
      </div>
    </div>
  </router-link>
</template>
