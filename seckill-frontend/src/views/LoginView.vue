<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const { login } = useAuth()
const toast = useToast()

const form = reactive({ phone: '', password: '' })
const errors = reactive({ phone: '', password: '' })
const loading = ref(false)
const serverError = ref(route.query.expired ? '登录已过期，请重新登录' : '')

const PHONE_RE = /^1[3-9]\d{9}$/

function validate(): boolean {
  errors.phone = ''
  errors.password = ''

  if (!form.phone.trim()) errors.phone = '请输入手机号'
  else if (!PHONE_RE.test(form.phone.trim())) errors.phone = '手机号格式不正确'

  if (!form.password) errors.password = '请输入密码'
  else if (form.password.length < 8 || form.password.length > 20) errors.password = '密码长度为 8-20 位'

  return !errors.phone && !errors.password
}

async function submit() {
  serverError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    await login({ phone: form.phone.trim(), password: form.password })
    toast.show('登录成功，欢迎回来', 'success')
    router.push((route.query.redirect as string) || '/products')
  } catch (e) {
    serverError.value = e instanceof Error ? e.message : '登录失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <aside class="auth-side">
        <span class="side-ghost">秒</span>
        <span class="side-seal">秒</span>
        <div class="side-body">
          <p class="side-kicker">SECKILL COLLECTION</p>
          <h2 class="side-title">限量好物<br />准时开抢</h2>
          <p class="side-desc">登录账号，第一时间锁定心仪商品。</p>
        </div>
        <p class="side-foot">秒杀专场 · 2026</p>
      </aside>

      <main class="auth-main">
        <h1>登录</h1>
        <p class="auth-sub">使用手机号登录你的账号</p>

        <div v-if="serverError" class="form-alert">{{ serverError }}</div>

        <form novalidate @submit.prevent="submit">
          <div class="field">
            <label class="field-label" for="phone"><span><i class="req">*</i>手机号</span></label>
            <input
              id="phone"
              v-model="form.phone"
              class="input"
              :class="{ err: errors.phone }"
              type="tel"
              maxlength="11"
              placeholder="请输入 11 位手机号"
              autocomplete="tel"
              @input="errors.phone = ''"
            />
            <p v-if="errors.phone" class="field-error">{{ errors.phone }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="password"><span><i class="req">*</i>密码</span></label>
            <input
              id="password"
              v-model="form.password"
              class="input"
              :class="{ err: errors.password }"
              type="password"
              maxlength="20"
              placeholder="请输入密码"
              autocomplete="current-password"
              @input="errors.password = ''"
            />
            <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
          </div>

          <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner"></span>{{ loading ? '登录中' : '登 录' }}
          </button>
        </form>

        <p class="auth-switch">还没有账号？<router-link :to="{ name: 'register' }">立即注册</router-link></p>
      </main>
    </div>
  </div>
</template>
