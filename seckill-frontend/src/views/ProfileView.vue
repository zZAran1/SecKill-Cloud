<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { maskEmail, maskPhone } from '@/utils/format'

const router = useRouter()
const { state, fetchUser, logout } = useAuth()
const toast = useToast()

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

/* ---------- 修改资料 ---------- */

const editing = ref(false)
const savingProfile = ref(false)
const profileError = ref('')

const profileForm = reactive({
  username: '',
  email: '',
  sex: '',
  age: '' as string | number,
  address: '',
})

function startEdit() {
  const u = user.value
  profileForm.username = u?.username ?? ''
  profileForm.email = u?.email ?? ''
  profileForm.sex = u?.sex ?? ''
  profileForm.age = u?.age ?? ''
  profileForm.address = u?.address ?? ''
  profileError.value = ''
  editing.value = true
}

async function saveProfile() {
  profileError.value = ''
  if (!profileForm.username.trim()) {
    profileError.value = '用户名不能为空'
    return
  }
  savingProfile.value = true
  try {
    await userApi.updateProfile({
      username: profileForm.username.trim(),
      email: profileForm.email.trim() || undefined,
      sex: profileForm.sex.trim() || undefined,
      age: profileForm.age === '' ? undefined : Number(profileForm.age),
      address: profileForm.address.trim() || undefined,
    })
    await fetchUser()
    editing.value = false
    toast.show('资料已更新', 'success')
  } catch (e) {
    profileError.value = e instanceof Error ? e.message : '保存失败，请稍后再试'
  } finally {
    savingProfile.value = false
  }
}

/* ---------- 修改密码 ---------- */

const changingPwd = ref(false)
const savingPwd = ref(false)
const pwdError = ref('')

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirm: '',
})

function startChangePwd() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
  pwdError.value = ''
  changingPwd.value = true
}

async function savePwd() {
  pwdError.value = ''
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    pwdError.value = '原密码和新密码都不能为空'
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 32) {
    pwdError.value = '新密码长度必须在 6-32 位之间'
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  savingPwd.value = true
  try {
    await userApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    changingPwd.value = false
    toast.show('密码修改成功，下次登录请使用新密码', 'success')
  } catch (e) {
    pwdError.value = e instanceof Error ? e.message : '修改失败，请稍后再试'
  } finally {
    savingPwd.value = false
  }
}

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
  try {
    await fetchUser()
  } catch {
    /* 401 已由全局拦截处理，其余错误保留缓存数据 */
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
          <button v-if="!editing" class="btn btn-line btn-sm" type="button" @click="startEdit">
            编辑资料
          </button>
        </div>

        <!-- 展示态 -->
        <dl v-if="!editing" class="info-grid">
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
            <dd>{{ user?.sex || '--' }}</dd>
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
            <dd>{{ maskEmail(user?.email) }}</dd>
          </div>
          <div class="info-cell">
            <dt>地址</dt>
            <dd>{{ user?.address || '--' }}</dd>
          </div>
        </dl>

        <!-- 编辑态 -->
        <div v-else class="profile-form">
          <div class="field">
            <label class="field-label" for="pf-username">
              <span><i class="req">*</i>用户名</span>
            </label>
            <input id="pf-username" v-model="profileForm.username" class="input" maxlength="50" />
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field-label" for="pf-email"><span>邮箱</span></label>
              <input id="pf-email" v-model="profileForm.email" class="input" type="email" maxlength="100" />
            </div>
            <div class="field">
              <label class="field-label" for="pf-sex"><span>性别</span></label>
              <input id="pf-sex" v-model="profileForm.sex" class="input" maxlength="10" />
            </div>
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field-label" for="pf-age"><span>年龄</span></label>
              <input id="pf-age" v-model="profileForm.age" class="input" inputmode="numeric" maxlength="3" />
            </div>
            <div class="field">
              <label class="field-label" for="pf-address"><span>地址</span></label>
              <input id="pf-address" v-model="profileForm.address" class="input" maxlength="255" />
            </div>
          </div>

          <p v-if="profileError" class="field-error">{{ profileError }}</p>

          <div class="form-actions">
            <button class="btn btn-primary btn-sm" type="button" :disabled="savingProfile" @click="saveProfile">
              <span v-if="savingProfile" class="spinner"></span>
              {{ savingProfile ? '保存中' : '保存' }}
            </button>
            <button class="btn btn-ghost btn-sm" type="button" :disabled="savingProfile" @click="editing = false">
              取消
            </button>
          </div>
        </div>
      </section>

      <section class="profile-panel">
        <div class="profile-panel-head">
          <h2>登录密码</h2>
          <button v-if="!changingPwd" class="btn btn-line btn-sm" type="button" @click="startChangePwd">
            修改密码
          </button>
        </div>

        <div v-if="changingPwd" class="profile-form">
          <div class="field">
            <label class="field-label" for="pf-old"><span><i class="req">*</i>原密码</span></label>
            <input id="pf-old" v-model="pwdForm.oldPassword" class="input" type="password" autocomplete="current-password" />
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field-label" for="pf-new">
                <span><i class="req">*</i>新密码</span>
                <em class="field-count">6-32 位</em>
              </label>
              <input id="pf-new" v-model="pwdForm.newPassword" class="input" type="password" autocomplete="new-password" />
            </div>
            <div class="field">
              <label class="field-label" for="pf-confirm"><span><i class="req">*</i>确认新密码</span></label>
              <input id="pf-confirm" v-model="pwdForm.confirm" class="input" type="password" autocomplete="new-password" />
            </div>
          </div>

          <p v-if="pwdError" class="field-error">{{ pwdError }}</p>

          <div class="form-actions">
            <button class="btn btn-primary btn-sm" type="button" :disabled="savingPwd" @click="savePwd">
              <span v-if="savingPwd" class="spinner"></span>
              {{ savingPwd ? '提交中' : '确认修改' }}
            </button>
            <button class="btn btn-ghost btn-sm" type="button" :disabled="savingPwd" @click="changingPwd = false">
              取消
            </button>
          </div>
        </div>
        <p v-else class="profile-hint">定期更换密码可以提升账号安全性。</p>
      </section>

      <div class="profile-actions">
        <button class="btn btn-danger-line" type="button" :disabled="loggingOut" @click="onLogout">
          <span v-if="loggingOut" class="spinner"></span>{{ loggingOut ? '正在退出' : '退出登录' }}
        </button>
      </div>
    </div>
  </div>
</template>
