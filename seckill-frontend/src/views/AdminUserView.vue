<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { userApi } from '@/api/user'
import PagerBar from '@/components/PagerBar.vue'
import type { UserInfo } from '@/types'
import { useAuth } from '@/stores/auth'
import { formatTime } from '@/utils/format'
import { useToast } from '@/utils/toast'

const toast = useToast()
const { state } = useAuth()

const users = ref<UserInfo[]>([])
const loading = ref(true)
const errorMsg = ref('')
const busyId = ref<number | null>(null)

const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)

function roleLabel(role: number): string {
  return role === 0 ? '超级管理员' : role === 1 ? '管理员' : '普通用户'
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await userApi.pageUsers({
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size,
    })
    users.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function switchPage(p: number) {
  page.value = p
  load()
}

/** 封禁 / 解封：服务端会同步写入网关黑名单，被封禁账号立即失效 */
async function toggleStatus(u: UserInfo) {
  if (busyId.value !== null) return
  const toDisabled = u.status === 1
  const self = u.id === state.user?.id
  if (self) {
    toast.show('不能封禁自己的账号', 'error')
    return
  }
  if (!toDisabled && !window.confirm(`确定解封「${u.username}」吗？`)) return
  if (toDisabled && !window.confirm(`确定封禁「${u.username}」吗？该账号将立即无法登录。`)) return

  busyId.value = u.id
  try {
    await userApi.updateUserStatus(u.id, { status: toDisabled ? 0 : 1 })
    u.status = toDisabled ? 0 : 1
    toast.show(toDisabled ? '账号已封禁' : '账号已解封', 'success')
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '操作失败，请稍后再试', 'error')
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="orders-page">
    <div class="container">
      <header class="page-head">
        <p class="hero-kicker">USER ADMIN</p>
        <h1 class="hero-title">用户管理</h1>
        <p class="hero-sub">查询用户、封禁或解封账号；封禁后该账号的登录令牌立即失效</p>
      </header>

      <section class="seckill-panel">
        <div class="search-bar">
          <div class="search-box">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.2-3.2" />
            </svg>
            <input
              v-model="keyword"
              class="input"
              placeholder="按用户名 / 手机号 / 邮箱搜索"
              @keyup.enter="search"
            />
          </div>
          <button class="btn btn-primary" type="button" @click="search">搜索</button>
        </div>

        <div v-if="loading" class="order-list">
          <div v-for="i in 3" :key="i" class="skel-card order-skel">
            <div class="skel skel-line" style="width: 40%"></div>
            <div class="skel skel-line short"></div>
          </div>
        </div>

        <div v-else-if="errorMsg" class="state-block">
          <div class="state-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 8v5M12 16.5h.01" />
            </svg>
          </div>
          <p class="state-title">加载失败</p>
          <p class="state-desc">{{ errorMsg }}</p>
          <button class="btn btn-line" type="button" @click="load">重新加载</button>
        </div>

        <div v-else-if="users.length === 0" class="state-block">
          <div class="state-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="8" r="3.5" />
              <path d="M5 19c0-3.3 3.1-5.5 7-5.5s7 2.2 7 5.5" />
            </svg>
          </div>
          <p class="state-title">没有匹配的用户</p>
          <p class="state-desc">换个关键词再试试</p>
        </div>

        <template v-else>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>手机号</th>
                  <th>邮箱</th>
                  <th>角色</th>
                  <th>状态</th>
                  <th>注册时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in users" :key="u.id">
                  <td class="num">{{ u.id }}</td>
                  <td>{{ u.username }}</td>
                  <td class="num">{{ u.phone || '--' }}</td>
                  <td>{{ u.email || '--' }}</td>
                  <td>{{ roleLabel(u.role) }}</td>
                  <td>
                    <span class="act-phase" :class="u.status === 1 ? 'on' : 'muted'">
                      {{ u.status === 1 ? '正常' : '已封禁' }}
                    </span>
                  </td>
                  <td class="num">{{ formatTime(u.createTime) }}</td>
                  <td>
                    <button
                      class="btn btn-sm"
                      :class="u.status === 1 ? 'btn-danger-line' : 'btn-line'"
                      type="button"
                      :disabled="busyId !== null || u.id === state.user?.id"
                      @click="toggleStatus(u)"
                    >
                      <span v-if="busyId === u.id" class="spinner"></span>
                      {{ u.status === 1 ? '封禁' : '解封' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <PagerBar :page="page" :size="size" :total="total" @change="switchPage" />
        </template>
      </section>
    </div>
  </div>
</template>
