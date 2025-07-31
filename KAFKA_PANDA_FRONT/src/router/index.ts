import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import Connections from '@/views/Connections.vue'
import Topics from '@/views/Topics.vue'
import Messages from '@/views/Messages.vue'
import Metrics from '@/views/Metrics.vue'
import TopicMonitoring from '@/views/TopicMonitoring.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
      path: '/messages',
      name: 'Messages',
      component: Messages,
      meta: { title: '메시지 관리' }
    },
    {
      path: '/metrics',
      name: 'Metrics',
      component: Metrics,
      meta: { title: '메트릭스' }
    },
    {
      path: '/topic-monitoring',
      name: 'TopicMonitoring',
      component: TopicMonitoring,
      meta: { title: '토픽 모니터링' }
    }
  ]
})

router.beforeEach((to, from, next) => {
  document.title = `Kafka Kopanda - ${to.meta['title']}`
  next()
})

export default router