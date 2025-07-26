import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import Connections from '@/views/Connections.vue'
import Topics from '@/views/Topics.vue'
import Messages from '@/views/Messages.vue'
import Metrics from '@/views/Metrics.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: Dashboard
    },
    {
      path: '/connections',
      name: 'Connections',
      component: Connections
    },
    {
      path: '/topics',
      name: 'Topics',
      component: Topics
    },
    {
      path: '/messages',
      name: 'Messages',
      component: Messages
    },
    {
      path: '/metrics',
      name: 'Metrics',
      component: Metrics
    }
  ]
})

export default router