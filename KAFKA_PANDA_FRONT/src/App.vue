<template>
  <div id="app">
    <el-container class="app-container">
      <!-- 사이드바 -->
      <el-aside width="250px" class="sidebar">
        <div class="logo-container">
          <img src="@/assets/logo.svg" alt="Kafka Panda" class="logo" @click="goToDashboard" />
          <h1 class="logo-text">Kafka Panda</h1>
        </div>
        
        <el-menu
          :default-active="$route.path"
          class="sidebar-menu"
          router
          @select="handleMenuSelect"
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
          
          <el-menu-item index="/topic-monitoring">
            <el-icon><DataAnalysis /></el-icon>
            <span>토픽 모니터링</span>
          </el-menu-item>
          
          <el-menu-item index="/messages">
            <el-icon><Message /></el-icon>
            <span>메시지 관리</span>
          </el-menu-item>
          
          <el-menu-item index="/metrics">
            <el-icon><TrendCharts /></el-icon>
            <span>메트릭</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 메인 콘텐츠 -->
      <el-container class="main-container">
        <el-header class="header">
          <div class="header-content">
            <div class="breadcrumb">
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/' }">홈</el-breadcrumb-item>
                <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
              </el-breadcrumb>
            </div>
            
            <div class="header-actions">
              <el-button @click="refreshCurrentPage" :loading="refreshing">
                <el-icon><Refresh /></el-icon>
                새로고침
              </el-button>
            </div>
          </div>
        </el-header>

        <el-main class="main-content">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <keep-alive>
                <component :is="Component" :key="$route.path" />
              </keep-alive>
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Monitor, 
  Connection, 
  Document, 
  DataAnalysis,
  Message, 
  TrendCharts, 
  Refresh 
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const refreshing = ref(false)

// 현재 페이지 제목
const currentPageTitle = computed(() => {
  const routeTitles: Record<string, string> = {
    '/': '대시보드',
    '/connections': '연결 관리',
    '/topics': '토픽 관리',
    '/topic-monitoring': '토픽 모니터링',
    '/messages': '메시지 관리',
    '/metrics': '메트릭'
  }
  return routeTitles[route.path] || '대시보드'
})

// 대시보드로 이동
const goToDashboard = () => {
  router.push('/')
}

// 메뉴 선택 처리
const handleMenuSelect = (index: string) => {
  console.log('메뉴 선택:', index)
  // 같은 라우트인 경우 강제로 리렌더링
  if (route.path === index) {
    router.replace('/temp').then(() => {
      router.replace(index)
    })
  }
}

// 현재 페이지 새로고침
const refreshCurrentPage = async () => {
  refreshing.value = true
  try {
    // 현재 라우트를 다시 로드
    const currentPath = route.path
    await router.replace('/temp')
    await router.replace(currentPath)
    ElMessage.success('페이지가 새로고침되었습니다.')
  } catch (error) {
    ElMessage.error('새로고침 중 오류가 발생했습니다.')
  } finally {
    refreshing.value = false
  }
}

// 라우트 변경 감지
watch(() => route.path, (newPath) => {
  console.log('라우트 변경:', newPath)
}, { immediate: true })

onMounted(() => {
  console.log('App 컴포넌트 마운트됨')
})
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #001529;
  color: #fff;
  border-right: 1px solid #435266;
}

.logo-container {
  display: flex;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #435266;
  cursor: pointer;
  transition: background-color 0.3s;
}

.logo-container:hover {
  background-color: #435266;
}

.logo {
  width: 32px;
  height: 32px;
  margin-right: 12px;
}

.logo-text {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.sidebar-menu {
  border-right: none;
  background-color: transparent;
}

.sidebar-menu .el-menu-item {
  color: #fff;
  border-bottom: 1px solid #435266;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #435266;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #409EFF;
  color: #fff;
}

.main-container {
  background-color: #f5f7fa;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.breadcrumb {
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.main-content {
  padding: 0;
  background-color: #f5f7fa;
}

/* 트랜지션 애니메이션 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 스크롤바 스타일링 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>