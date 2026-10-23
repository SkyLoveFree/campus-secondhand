import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

// 应用启动时通过 /api/auth/me 恢复登录态；游客（401）静默处理，避免刷新后误判为已登录
useUserStore(pinia).restore()

app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
