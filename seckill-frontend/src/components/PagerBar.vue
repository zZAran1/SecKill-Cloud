<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  size: number
  total: number
}>()

const emit = defineEmits<{ change: [page: number] }>()

const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

function go(p: number) {
  if (p < 1 || p > pages.value || p === props.page) return
  emit('change', p)
}
</script>

<template>
  <nav v-if="total > 0" class="pager">
    <button class="pager-btn" type="button" :disabled="page <= 1" @click="go(page - 1)">
      上一页
    </button>
    <span class="pager-info">
      第 <em>{{ page }}</em> / {{ pages }} 页 · 共 {{ total }} 条
    </span>
    <button class="pager-btn" type="button" :disabled="page >= pages" @click="go(page + 1)">
      下一页
    </button>
  </nav>
</template>
