import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

// 所有请求统一走这里：baseURL 固定 /api，由 Vite 代理转发到后端，前端不硬编码后端地址
const request = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 10000,
})

const DEFAULT_ERROR_MESSAGE = '请求失败，请稍后重试'
// 登录/注册页的 401（如登录密码错误）由页面内联提示，不做跳转
const AUTH_PAGE_NAMES = ['login', 'register']

// 把 axios 错误/业务错误统一成 { code, message, data, status } 结构，供页面按 code 处理
function normalizeError(rawError) {
  const response = rawError?.response
  const body = response?.data
  return {
    code: typeof body?.code === 'number' ? body.code : -1,
    message:
      typeof body?.message === 'string' && body.message.trim()
        ? body.message
        : DEFAULT_ERROR_MESSAGE,
    data: body?.data ?? null,
    status: response?.status ?? 0,
  }
}

// HTTP 400 时优先展示后端 fieldErrors 中的第一条中文说明
function firstFieldError(data) {
  const fieldErrors = data?.fieldErrors
  if (!fieldErrors || typeof fieldErrors !== 'object') return ''
  return Object.values(fieldErrors).find((message) => typeof message === 'string' && message) || ''
}

// 401：清理登录态；/api/auth/me 静默调用（silentAuthError）时不弹提示、不跳转
async function handleUnauthorized(config) {
  useUserStore().clear()
  if (config?.silentAuthError) return

  const currentRoute = router.currentRoute.value
  if (AUTH_PAGE_NAMES.includes(currentRoute.name)) return

  ElMessage.warning('登录已过期，请重新登录')
  const redirect = currentRoute.fullPath
  try {
    await router.replace({
      name: 'login',
      query: redirect && redirect !== '/' ? { redirect } : {},
    })
  } catch {
    // 跳转失败（如导航被中断）不打断请求错误向上传递，用户下一次操作仍会收到 401
  }
}

function handleBusinessError(error, config) {
  switch (error.status) {
    case 401:
      handleUnauthorized(config)
      break
    case 403:
      ElMessage.error(error.message || '无权限执行该操作')
      break
    case 400:
      ElMessage.error(firstFieldError(error.data) || error.message)
      break
    case 500:
      ElMessage.error('服务器异常，请稍后重试')
      break
    default:
      // 404/405/409 与网络错误由调用页面按场景展示（如详情页 404 空态、注册页 409 用户名重复）
      break
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 成功：HTTP 200 且 code === 0，直接返回 data.data
    if (body && body.code === 0) return body.data

    // 防御式处理：HTTP 200 但业务码非 0（契约要求失败用 4xx/5xx，正常不会走到这里）
    const error = {
      code: typeof body?.code === 'number' ? body.code : -1,
      message:
        typeof body?.message === 'string' && body.message.trim()
          ? body.message
          : DEFAULT_ERROR_MESSAGE,
      data: body?.data ?? null,
      status: response.status,
    }
    handleBusinessError(error, response.config)
    return Promise.reject(error)
  },
  (rawError) => {
    const error = normalizeError(rawError)
    handleBusinessError(error, rawError?.config)
    return Promise.reject(error)
  }
)

export default request
