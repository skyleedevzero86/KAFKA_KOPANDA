<template>
  <div id="app">
    <el-container class="app-container">
      <el-aside width="250px" class="sidebar">
        <div class="logo">
          <h2>Kafka Kopanda</h2>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/">
            <el-icon><Monitor /></el-icon>
            <span>대시보드</span>
          </el-menu-item>
          <el-menu-item index="/connections">
            <el-icon><Connection /></el-icon>
            <span>연결 관리</span>
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
import { useRoute } from 'vue-router'
import { Monitor, Connection, Document, Message, DataAnalysis, TrendCharts, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
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
}

.logo h2 {
  margin: 0;
  color: #409EFF;
}

.sidebar-menu {
  border-right: none;
  background-color: transparent;
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
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>