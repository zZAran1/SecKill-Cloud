<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { maskPhone } from '@/utils/format'

const router = useRouter()
const { state, fetchUser, logout } = useAuth()
const toast = useToast()

const loading = ref(false)
const loggingOut = ref(false)

const user = computed(() => state.user)

const roleLabel = computed(() => {
  switch (user.value?.role) {
    case 0:
      return '超级管理员'
    case 1:
      return '管理员'
    default:
      return '普通用户'
  }
})

const statusLabel = computed(() => (user.value?.status === 1 ? '正常' : '已封禁'))

async function onLogout() {
  loggingOut.value = true
  try {
    await logout()
    toast.show('已退出登录', 'info')
    router.push({ name: 'login' })
  } finally {
    loggingOut.value = false
  }
}

// 进入页面时刷新一次最新资料
onMounted(async () => {
  loading.value = true
  try {
    await fetchUser()
  } catch {
    /* 401 已由全局拦截处理，其余错误保留缓存数据 */
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="profile-page">
    <div class="container">
      <section class="profile-hero">
        <span class="side-ghost">秒</span>
        <div class="profile-avatar">{{ (user?.username ?? '我').slice(0, 1) }}</div>
        <div class="profile-who">
          <h1 class="profile-name">{{ user?.username ?? '加载中' }}</h1>
          <div class="profile-badges">
            <span class="badge">{{ roleLabel }}</span>
            <span class="badge">账号{{ statusLabel }}</span>
          </div>
        </div>
      </section>

      <section class="profile-panel">
        <div class="profile-panel-head">
          <h2>账号资料</h2>
        </div>
        <dl class="info-grid">
          <div class="info-cell">
            <dt>用户 ID</dt>
            <dd>{{ user?.id ?? '--' }}</dd>
          </div>
          <div class="info-cell">
            <dt>用户名</dt>
            <dd>{{ user?.username ?? '--' }}</dd>
          </div>
          <div class="info-cell">
            <dt>性别</dt>
            <dd>{{ user?.sex ?? '--' }}</dd>
          </div>
          <div class="info-cell">
            <dt>年龄</dt>
            <dd>{{ user?.age ?? '--' }}</dd>
          </div>
          <div class="info-cell">
            <dt>手机号</dt>
            <dd>{{ maskPhone(user?.phone) }}</dd>
          </div>
          <div class="info-cell">
            <dt>邮箱</dt>
            <dd>{{ user?.email ?? '--' }}</dd>
          </div>
        </dl>
      </section>

      <div class="profile-actions">
        <button class="btn btn-danger-line" type="button" :disabled="loggingOut" @click="onLogout">
          <span v-if="loggingOut" class="spinner"></span>{{ loggingOut ? '正在退出' : '退出登录' }}
        </button>
      </div>
    </div>
  </div>
</template>
