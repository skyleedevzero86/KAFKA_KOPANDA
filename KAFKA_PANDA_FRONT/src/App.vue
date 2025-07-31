<template>
  <div id="app">
    <el-container class="app-container">
      <el-aside width="250px" class="sidebar">
        <div class="logo" @click="goHome">
          <h2>Kafka Kopanda</h2>
        </div>
        <el-menu
          :default-active="currentPath"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/connections">
            <el-icon><Connection /></el-icon>
            <span>연결 관리</span>
          </el-menu-item>
          <el-menu-item index="/">
            <el-icon><Monitor /></el-icon>
            <span>대시보드</span>
          </el-menu-item>
          <el-menu-item index="/topics">
            <el-icon><Document /></el-icon>
            <span>토픽 관리</span>
          </el-menu-item>
          <el-menu-item index="/topic-monitoring">
            <el-icon><DataAnalysis /></el-icon>
            <span>토픽 모니터링</span>
          </el-menu-item>
          <el-menu-item index="/messages">
            <el-icon><ChatDotRound /></el-icon>
            <span>메시지 관리</span>
          </el-menu-item>
          <el-menu-item index="/metrics">
            <el-icon><TrendCharts /></el-icon>
            <span>메트릭스</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <el-header class="header">
          <div class="header-content">
            <div class="breadcrumb">
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/' }">홈</el-breadcrumb-item>
                <el-breadcrumb-item v-if="currentRouteName !== 'Dashboard'">
                  {{ getPageTitle() }}
                </el-breadcrumb-item>
              </el-breadcrumb>
            </div>
            <div class="header-actions">
              <el-button @click="refreshCurrentPage" size="small">
                <el-icon><Refresh /></el-icon>
                새로고침
              </el-button>
            </div>
          </div>
        </el-header>
        
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Connection, 
  Monitor, 
  Document, 
  DataAnalysis, 
  ChatDotRound, 
  TrendCharts,
  Refresh
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 안전한 라우트 접근을 위한 computed 속성들
const currentPath = computed(() => route?.path || '/')
const currentRouteName = computed(() => route?.name || 'Dashboard')

const goHome = () => {
  router.push('/')
}

const refreshCurrentPage = () => {
  window.location.reload()
}

const getPageTitle = () => {
  const routeNames: Record<string, string> = {
    'Connections': '연결 관리',
    'Topics': '토픽 관리',
    'TopicMonitoring': '토픽 모니터링',
    'Messages': '메시지 관리',
    'Metrics': '메트릭스'
  }
  return routeNames[currentRouteName.value as string] || '페이지'
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: white;
  border-right: 1px solid #e4e7ed;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #435266;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logo:hover {
  background-color: #435266;
}

.logo h2 {
  margin: 0;
  color: white;
  font-size: 1.5rem;
  font-weight: 600;
}

.sidebar-menu {
  border-right: none;
  background-color: transparent;
}

.sidebar-menu :deep(.el-menu-item) {
  color: #c0c4cc;
  font-size: 14px;
  font-weight: 500;
  height: 50px;
  line-height: 50px;
  transition: all 0.3s ease;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  color: #ffffff;
  background-color: #435266;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #409EFF;
  background-color: #263445;
  font-weight: 600;
  font-size: 15px;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 16px;
}

.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #409EFF;
}

.header {
  background-color: white;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  display: flex;
  align-items: center;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.breadcrumb {
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px !important;
  }
  
  .header-content {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>