<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Product } from '@/types'
import { formatPrice } from '@/utils/format'
import { safeImageUrl } from '@/utils/security'

const props = defineProps<{ product: Product }>()

const imgFailed = ref(false)
const img = computed(() => safeImageUrl(props.product.imageUrl))
</script>

<template>
  <router-link :to="`/products/${props.product.id}`" class="p-card">
    <div class="p-media">
      <img
        v-if="img && !imgFailed"
        :src="img"
        :alt="props.product.name"
        loading="lazy"
        @error="imgFailed = true"
      />
      <div v-else class="p-fallback">
        <span>{{ props.product.name.slice(0, 1) }}</span>
      </div>
      <span class="p-tag">{{ props.product.status === 1 ? '在售' : '已下架' }}</span>
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
