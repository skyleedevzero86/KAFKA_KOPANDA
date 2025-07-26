<template>
  <div id="app">
    <el-container class="app-container">
      <el-aside width="250px" class="sidebar">
        <div class="logo" @click="goToHome">
          <h2>🚀 Kafka Kopanda</h2>
        </div>
        <el-menu
          :default-active="$route.path"
          class="sidebar-menu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
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
            <el-icon><ChatDotRound /></el-icon>
            <span>메시지 관리</span>
          </el-menu-item>
          <el-menu-item index="/metrics">
            <el-icon><TrendCharts /></el-icon>
            <span>메트릭</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <el-header class="header">
          <div class="header-content">
            <h3>{{ pageTitle }}</h3>
            <div class="header-actions">
              <el-button type="primary" @click="refreshData">
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
import { Monitor, Connection, Document, ChatDotRound, TrendCharts, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/': '대시보드',
    '/connections': '연결 관리',
    '/topics': '토픽 관리',
    '/messages': '메시지 관리',
    '/metrics': '메트릭'
  }
  return titles[route.path] || 'Kafka Kopanda'
})

const goToHome = () => {
  router.push('/')
}

const refreshData = () => {
  window.location.reload()
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: #bfcbd9;
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
  font-size: 18px;
}

.sidebar-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
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

.header-actions {
  display: flex;
  gap: 10px;
}
</style>