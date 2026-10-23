<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loggingOut = ref(false)

const roleLabel = computed(() => (userStore.isAdmin ? '管理员' : '普通用户'))

async function handleLogout() {
  if (loggingOut.value) return
  loggingOut.value = true

  try {
    await logout()
  } catch (error) {
    // 会话已失效（401/10002）时后端同样视为已登出，继续清理本地状态即可
    if (error?.code !== 10002) {
      ElMessage.error(error?.message || '退出登录失败')
    }
  } finally {
    userStore.clear()
    loggingOut.value = false
    ElMessage.success('已退出登录')
    router.replace({ name: 'login' })
  }
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <router-link to="/items" class="app-header__brand">校园二手交易</router-link>

      <nav class="app-header__nav">
        <router-link to="/items" class="app-header__link">商品列表</router-link>
      </nav>

      <div class="app-header__actions">
        <template v-if="userStore.isLoggedIn">
          <span class="app-header__user">
            {{ userStore.user.username }}
            <el-tag size="small" type="info">{{ roleLabel }}</el-tag>
          </span>
          <router-link to="/publish" class="app-header__link">发布商品</router-link>
          <el-button size="small" :loading="loggingOut" @click="handleLogout">退出登录</el-button>
        </template>

        <template v-else>
          <router-link to="/login" class="app-header__link">登录</router-link>
          <router-link to="/register" class="app-header__link">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>
