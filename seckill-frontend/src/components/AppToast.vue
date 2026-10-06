<script setup lang="ts">
import { useToast } from '@/utils/toast'

const { state } = useToast()
</script>

<template>
  <teleport to="body">
    <div class="toast-wrap">
      <transition-group name="toast">
        <div v-for="t in state.list" :key="t.id" class="toast" :class="`toast-${t.type}`">
          <svg v-if="t.type === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="9" />
            <path d="m8.5 12.2 2.4 2.4 4.6-5" />
          </svg>
          <svg v-else-if="t.type === 'error'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="9" />
            <path d="m9 9 6 6M15 9l-6 6" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 11v5M12 8h.01" />
          </svg>
          <span>{{ t.msg }}</span>
        </div>
      </transition-group>
    </div>
  </teleport>
</template>
