import { defineStore } from 'pinia'
import { getCurrentUser } from '@/api/auth'

// 登录态恢复只发起一次请求：路由守卫与 main.js 可能同时调用 restore()
let restorePromise = null

export const useUserStore = defineStore('user', {
  state: () => ({
    // 当前登录用户：{ id, username, role }，未登录为 null
    user: null,
    // 是否已尝试过恢复登录态，避免重复请求 /api/auth/me
    restored: false,
  }),

  getters: {
    isLoggedIn: (state) => state.user !== null,
    isAdmin: (state) => state.user?.role === 'ADMIN',
  },

  actions: {
    setUser(user) {
      this.user = user
    },

    clear() {
      this.user = null
    },

    // 刷新后从后端恢复登录态；未登录（401）时静默视为游客
    async restore() {
      if (this.restored) return this.user

      if (!restorePromise) {
        restorePromise = getCurrentUser()
          .then((user) => {
            this.user = user
            return user
          })
          .catch(() => {
            this.user = null
            return null
          })
          .finally(() => {
            this.restored = true
            restorePromise = null
          })
      }

      return restorePromise
    },
  },
})
