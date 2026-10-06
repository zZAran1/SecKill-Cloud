import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    requiresAdmin?: boolean
    guestOnly?: boolean
    /** 沉浸式页面，不渲染公共头部 */
    plain?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/products' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', guestOnly: true, plain: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '注册', guestOnly: true, plain: true },
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('@/views/ProductListView.vue'),
      meta: { title: '全部商品' },
    },
    {
      path: '/products/new',
      name: 'product-new',
      component: () => import('@/views/ProductCreateView.vue'),
      meta: { title: '发布商品', requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/products/:id(\\d+)',
      name: 'product-detail',
      component: () => import('@/views/ProductDetailView.vue'),
      meta: { title: '商品详情' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { title: '个人中心', requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const { state, isLoggedIn, isAdmin, fetchUser } = useAuth()

  if (to.meta.guestOnly && isLoggedIn.value) {
    return { name: 'products' }
  }

  if (to.meta.requiresAuth && !isLoggedIn.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin) {
    if (isLoggedIn.value && !state.user) {
      try {
        await fetchUser()
      } catch {
        /* 拉取失败按非管理员处理 */
      }
    }
    if (!isAdmin.value) {
      return { name: 'products' }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 秒杀专场` : '秒杀专场'
})

export default router
