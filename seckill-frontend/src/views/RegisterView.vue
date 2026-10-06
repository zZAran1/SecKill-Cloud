<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/utils/toast'

const router = useRouter()
const { register } = useAuth()
const toast = useToast()

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirm: '',
  age: null as number | null,
  sex: '',
})

const errors = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirm: '',
  age: '',
  sex: '',
})

const loading = ref(false)
const serverError = ref('')

const PHONE_RE = /^1[3-9]\d{9}$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
// 8-20 位，须含字母和数字，不含空白（与后端校验一致）
const PWD_RE = /^(?=.*[A-Za-z])(?=.*\d)\S{8,20}$/

function validate(): boolean {
  Object.keys(errors).forEach((k) => (errors[k as keyof typeof errors] = ''))

  const name = form.username.trim()
  if (!name) errors.username = '请输入用户名'
  else if (name.length < 2 || name.length > 20) errors.username = '用户名长度为 2-20 个字符'

  if (!form.email.trim()) errors.email = '请输入邮箱'
  else if (!EMAIL_RE.test(form.email.trim())) errors.email = '邮箱格式不正确'

  if (!form.phone.trim()) errors.phone = '请输入手机号'
  else if (!PHONE_RE.test(form.phone.trim())) errors.phone = '手机号格式不正确'

  if (!form.password) errors.password = '请输入密码'
  else if (!PWD_RE.test(form.password)) errors.password = '密码需 8-20 位，同时包含字母和数字，不含空格'

  if (!form.confirm) errors.confirm = '请再次输入密码'
  else if (form.confirm !== form.password) errors.confirm = '两次输入的密码不一致'

  if (form.age === null || form.age === undefined) errors.age = '请输入年龄'
  else if (!Number.isInteger(form.age) || form.age < 1 || form.age > 120) errors.age = '年龄需为 1-120 的整数'

  if (!form.sex) errors.sex = '请选择性别'

  return Object.values(errors).every((v) => !v)
}

async function submit() {
  serverError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    await register({
      username: form.username.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      password: form.password,
      age: form.age as number,
      sex: form.sex,
    })
    toast.show('注册成功，请登录', 'success')
    router.push({ name: 'login' })
  } catch (e) {
    serverError.value = e instanceof Error ? e.message : '注册失败，请稍后再试'
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
          <h2 class="side-title">加入专场<br />好物不错过</h2>
          <p class="side-desc">注册账号，即可参与每一场限时秒杀。</p>
        </div>
        <p class="side-foot">秒杀专场 · 2026</p>
      </aside>

      <main class="auth-main">
        <h1>注册</h1>
        <p class="auth-sub">创建你的秒杀专场账号</p>

        <div v-if="serverError" class="form-alert">{{ serverError }}</div>

        <form novalidate @submit.prevent="submit">
          <div class="field">
            <label class="field-label" for="username"><span><i class="req">*</i>用户名</span></label>
            <input
              id="username"
              v-model="form.username"
              class="input"
              :class="{ err: errors.username }"
              maxlength="20"
              placeholder="2-20 个字符"
              autocomplete="username"
              @input="errors.username = ''"
            />
            <p v-if="errors.username" class="field-error">{{ errors.username }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="email"><span><i class="req">*</i>邮箱</span></label>
            <input
              id="email"
              v-model="form.email"
              class="input"
              :class="{ err: errors.email }"
              type="email"
              placeholder="name@example.com"
              autocomplete="email"
              @input="errors.email = ''"
            />
            <p v-if="errors.email" class="field-error">{{ errors.email }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="rphone"><span><i class="req">*</i>手机号</span></label>
            <input
              id="rphone"
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
            <label class="field-label" for="rpassword"><span><i class="req">*</i>密码</span></label>
            <input
              id="rpassword"
              v-model="form.password"
              class="input"
              :class="{ err: errors.password }"
              type="password"
              maxlength="20"
              placeholder="8-20 位，含字母和数字"
              autocomplete="new-password"
              @input="errors.password = ''"
            />
            <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="confirm"><span><i class="req">*</i>确认密码</span></label>
            <input
              id="confirm"
              v-model="form.confirm"
              class="input"
              :class="{ err: errors.confirm }"
              type="password"
              maxlength="20"
              placeholder="再次输入密码"
              autocomplete="new-password"
              @input="errors.confirm = ''"
            />
            <p v-if="errors.confirm" class="field-error">{{ errors.confirm }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="age"><span><i class="req">*</i>年龄</span></label>
            <input
              id="age"
              v-model.number="form.age"
              class="input"
              :class="{ err: errors.age }"
              type="number"
              min="1"
              max="120"
              placeholder="1-120"
              @input="errors.age = ''"
            />
            <p v-if="errors.age" class="field-error">{{ errors.age }}</p>
          </div>

          <div class="field">
            <span class="field-label"><span><i class="req">*</i>性别</span></span>
            <div class="radio-row">
              <label class="radio-pill">
                <input v-model="form.sex" type="radio" value="男" @change="errors.sex = ''" />
                <span>男</span>
              </label>
              <label class="radio-pill">
                <input v-model="form.sex" type="radio" value="女" @change="errors.sex = ''" />
                <span>女</span>
              </label>
            </div>
            <p v-if="errors.sex" class="field-error">{{ errors.sex }}</p>
          </div>

          <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner"></span>{{ loading ? '注册中' : '注 册' }}
          </button>
        </form>

        <p class="auth-switch">已有账号？<router-link :to="{ name: 'login' }">直接登录</router-link></p>
      </main>
    </div>
  </div>
</template>
