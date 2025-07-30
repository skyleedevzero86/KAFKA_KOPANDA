import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import Connections from '@/views/Connections.vue'
import Topics from '@/views/Topics.vue'
import TopicMonitoring from '@/views/TopicMonitoring.vue'
import Messages from '@/views/Messages.vue'
import Metrics from '@/views/Metrics.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: Dashboard,
      meta: { title: '대시보드' }
    },
    {
      path: '/connections',
      name: 'Connections',
      component: Connections,
      meta: { title: '연결 관리' }
    },
    {
      path: '/topics',
      name: 'Topics',
      component: Topics,
      meta: { title: '토픽 관리' }
    },
    {
      path: '/topic-monitoring',
      name: 'TopicMonitoring',
      component: TopicMonitoring,
      meta: { title: '토픽 모니터링' }
    },
    {
      path: '/messages',
      name: 'Messages',
      component: Messages,
      meta: { title: '메시지 관리' }
    },
    {
      path: '/metrics',
      name: 'Metrics',
      component: Metrics,
      meta: { title: '메트릭' }
    },
    {
      path: '/temp',
      name: 'Temp',
      component: { template: '<div></div>' },
      meta: { title: '임시' }
    }
  ]
})

// 라우터 가드 추가
router.beforeEach((to, from, next) => {
  console.log('라우터 가드:', { from: from.path, to: to.path })
  next()
})

router.afterEach((to) => {
  console.log('라우터 완료:', to.path)
  // 페이지 제목 설정
  if (to.meta.title) {
    document.title = `Kafka Panda - ${to.meta.title}`
  }
})

export default router