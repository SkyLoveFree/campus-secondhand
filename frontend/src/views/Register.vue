<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

function validateConfirmPassword(rule, value, callback) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

// 规则与 docs/api-spec.md §3.1 一致：用户名 3–20 位字母/数字/下划线，密码 6–20 位且无空白
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{3,20}$/,
      message: '用户名为 3–20 位字母、数字或下划线',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^\S{6,20}$/,
      message: '密码为 6–20 位且不能包含空白字符',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
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
    await register({
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('注册成功，请登录')
    await router.replace({ name: 'login' })
  } catch (error) {
    if (error?.code === 10006) {
      ElMessage.error('用户名已存在，请更换一个')
    } else if (error?.code !== 10001) {
      // 400 参数校验错误已由请求层按 fieldErrors 提示，这里不重复提示
      ElMessage.error(error?.message || '注册失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <el-card class="auth-card">
      <h1 class="auth-card__title">注册</h1>
      <p class="auth-card__hint">注册普通用户账号后即可发布二手商品</p>

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
            placeholder="3–20 位字母、数字或下划线"
            maxlength="20"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="6–20 位，不能包含空格"
            maxlength="20"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            maxlength="20"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="auth-card__submit"
          :loading="submitting"
          native-type="submit"
        >
          注册
        </el-button>
      </el-form>

      <p class="auth-card__footer">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </p>
    </el-card>
  </section>
</template>
