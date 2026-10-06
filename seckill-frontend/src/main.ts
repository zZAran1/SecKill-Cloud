import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from '@/api/http'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/utils/toast'

const app = createApp(App)
app.use(router)

// 登录态失效：清理本地状态，受保护页面自动跳回登录页
setUnauthorizedHandler(() => {
  const { clear } = useAuth()
  const { show } = useToast()
  clear()
  const current = router.currentRoute.value
  show('登录已过期，请重新登录', 'info')
  if (current.meta.requiresAuth) {
    router.push({ name: 'login', query: { redirect: current.fullPath, expired: '1' } })
  }
})

app.mount('#app')
