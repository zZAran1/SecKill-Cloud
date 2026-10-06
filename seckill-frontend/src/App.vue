<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppToast from '@/components/AppToast.vue'

const route = useRoute()
const plain = computed(() => !!route.meta.plain)
</script>

<template>
  <div class="app-shell">
    <AppHeader v-if="!plain" />
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </transition>
    </router-view>
    <AppToast />
  </div>
</template>
