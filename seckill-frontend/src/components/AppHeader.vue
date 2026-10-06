<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/utils/toast'

const router = useRouter()
const { state, isLoggedIn, isAdmin, logout } = useAuth()
const toast = useToast()

async function onLogout() {
  await logout()
  toast.show('已退出登录', 'info')
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="site-header">
    <div class="container header-inner">
      <router-link :to="{ name: 'products' }" class="brand">
        <span class="brand-seal">秒</span>
        <span class="brand-text">
          <strong>秒杀专场</strong>
          <em>SECKILL COLLECTION</em>
        </span>
      </router-link>

      <nav class="nav">
        <router-link :to="{ name: 'products' }" class="nav-link" active-class="on">全部商品</router-link>
        <router-link v-if="isAdmin" :to="{ name: 'product-new' }" class="nav-link" active-class="on">发布商品</router-link>
      </nav>

      <div class="header-side">
        <template v-if="isLoggedIn">
          <router-link :to="{ name: 'profile' }" class="user-chip">
            <span class="avatar">{{ (state.user?.username ?? '我').slice(0, 1) }}</span>
            <span class="uname">{{ state.user?.username ?? '个人中心' }}</span>
          </router-link>
          <button class="btn btn-line btn-sm" type="button" @click="onLogout">退出</button>
        </template>
        <template v-else>
          <router-link :to="{ name: 'login' }" class="btn btn-ghost btn-sm">登录</router-link>
          <router-link :to="{ name: 'register' }" class="btn btn-primary btn-sm">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>
