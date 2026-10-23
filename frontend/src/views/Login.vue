<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '用户名不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 64, message: '密码不能超过 64 个字符', trigger: 'blur' },
  ],
}

// 只允许站内路径，避免 redirect 参数被利用做外链跳转
function resolveRedirect() {
  const redirect = route.query.redirect
  if (typeof redirect !== 'string') return '/items'
  if (!redirect.startsWith('/') || redirect.startsWith('//')) return '/items'
  return redirect
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const user = await login({
      username: form.username.trim(),
      password: form.password,
    })
    userStore.setUser(user)
    ElMessage.success('登录成功')
    await router.replace(resolveRedirect())
  } catch (error) {
    if (error?.code === 10003) {
      // 登录失败统一提示，不区分账号不存在 / 密码错误
      ElMessage.error('用户名或密码错误')
      form.password = ''
    } else if (error?.code !== 10001) {
      // 400 参数校验错误已由请求层按 fieldErrors 提示，这里不重复提示
      ElMessage.error(error?.message || '登录失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <el-card class="auth-card">
      <h1 class="auth-card__title">登录</h1>
      <p class="auth-card__hint">登录后可发布、修改和删除自己的商品</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            maxlength="50"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            maxlength="64"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="auth-card__submit"
          :loading="submitting"
          native-type="submit"
        >
          登录
        </el-button>
      </el-form>

      <p class="auth-card__footer">
        还没有账号？
        <router-link to="/register">去注册</router-link>
      </p>
    </el-card>
  </section>
</template>
