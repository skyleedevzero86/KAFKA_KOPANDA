<template>
  <div id="app">
    <el-container class="app-container">
      <el-aside width="250px" class="sidebar">
        <div class="logo" @click="goHome">
          <h2>Kafka Kopanda</h2>
        </div>
        <el-menu
          :default-active="$route.path"
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
          <el-menu-item index="/messages">
            <el-icon><Message /></el-icon>
            <span>메시지 관리</span>
          </el-menu-item>
          <el-menu-item index="/metrics">
            <el-icon><DataAnalysis /></el-icon>
            <span>메트릭스</span>
          </el-menu-item>
          <el-menu-item index="/topic-monitoring">
            <el-icon><TrendCharts /></el-icon>
            <span>토픽 모니터링</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <el-header class="header">
          <div class="header-content">
            <h3>{{ getPageTitle() }}</h3>
            <div class="header-actions">
              <el-button @click="refreshCurrentPage" :loading="loading">
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Monitor, Connection, Document, Message, DataAnalysis, TrendCharts, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const getPageTitle = () => {
  const titles: Record<string, string> = {
    '/': '대시보드',
    '/connections': '연결 관리',
    '/topics': '토픽 관리',
    '/messages': '메시지 관리',
    '/metrics': '메트릭스',
    '/topic-monitoring': '토픽 모니터링'
  }
  return titles[route.path] || 'Kafka Kopanda'
}

const goHome = () => {
  router.push('/')
}

const refreshCurrentPage = () => {
  loading.value = true
  setTimeout(() => {
    window.location.reload()
  }, 500)
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: white;
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
  color: #409EFF;
  font-size: 1.5rem;
  font-weight: 600;
}

.sidebar-menu {
  border-right: none;
  background-color: transparent;
}

/* 메뉴 아이템 스타일 개선 */
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
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-content h3 {
  margin: 0;
  color: #303133;
  font-weight: 600;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>