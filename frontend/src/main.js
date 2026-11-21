import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { useBrandStore } from './store/brand'
import './assets/styles/main.css'

const app = createApp(App)

// Pinia 状态管理
const pinia = createPinia()
app.use(pinia)

// 启动时加载管理员配置的机构品牌（名称/Logo），全站统一
useBrandStore(pinia).load()

// Vue Router
app.use(router)

// Element Plus 组件库（中文本地化：分页、空状态、日期选择等内置文案）
app.use(ElementPlus, { locale: zhCn })

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
