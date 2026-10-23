import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由统一使用 hash 模式（/#/items）：构建产物作为静态资源部署时刷新不会 404，后端无需 SPA fallback
const routes = [
  { path: '/', redirect: '/items' },
  {
    path: '/items',
    name: 'item-list',
    component: () => import('@/views/ItemList.vue'),
  },
  {
    path: '/items/:id',
    name: 'item-detail',
    component: () => import('@/views/ItemDetail.vue'),
  },
  {
    path: '/items/:id/edit',
    name: 'item-edit',
    component: () => import('@/views/ItemForm.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/publish',
    name: 'item-publish',
    component: () => import('@/views/ItemForm.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
  },
  { path: '/:pathMatch(.*)*', redirect: '/items' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 首次进入或刷新页面时恢复登录态（restore 内部只真正请求一次 /api/auth/me）
  if (!userStore.restored) {
    await userStore.restore()
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
