import request from '@/utils/request'

// 接口契约见 docs/api-spec.md §3.1–3.4；页面只通过这些函数访问后端，不直接拼 URL
export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getCurrentUser() {
  // 应用启动时静默调用：未登录返回 401 属于正常游客状态，不弹提示、不跳转
  return request.get('/auth/me', { silentAuthError: true })
}
