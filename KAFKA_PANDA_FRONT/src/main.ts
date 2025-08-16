import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import Chart from 'chart.js/auto'

import App from './App.vue'
import router from './router'

// Chart.js 등록
Chart.register()

const app = createApp(App)

// Pinia 스토어 등록
app.use(createPinia())

// 라우터 등록
app.use(router)

// Element Plus 등록
app.use(ElementPlus)

// Element Plus 아이콘들 전역 등록
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')