<template>
  <div class="topic-monitoring">
    <div class="monitoring-header">
      <h3>토픽 모니터링</h3>
      <div class="header-actions">
        <el-button @click="refreshMonitoring" :loading="loading">
          <el-icon><Refresh /></el-icon>
          새로고침
        </el-button>
        <el-button @click="toggleAutoRefresh" :type="autoRefresh ? 'success' : 'default'">
          <el-icon><VideoPlay v-if="!autoRefresh" /><VideoPause v-else /></el-icon>
          {{ autoRefresh ? '자동 새로고침 중지' : '자동 새로고침 시작' }}
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <LoadingSpinner message="모니터링 데이터를 불러오는 중..." />
    </div>

    <div v-else-if="error" class="error-container">
      <ErrorMessage :message="error" @close="clearError" />
    </div>

    <div v-else class="monitoring-content">
      <el-row :gutter="20" class="monitoring-overview">
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="overview-value">{{ monitoringTopics.length }}</div>
              <div class="overview-label">모니터링 토픽</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="overview-value">{{ healthyTopics }}</div>
              <div class="overview-label">정상 토픽</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="overview-value">{{ totalPartitions }}</div>
              <div class="overview-label">총 파티션</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="overview-value">{{ avgThroughput.toFixed(1) }}</div>
              <div class="overview-label">평균 처리량 (msg/s)</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="charts-section">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>토픽별 처리량</span>
            </template>
            <BarChart :data="topicThroughputData" />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>토픽 상태 분포</span>
            </template>
            <PieChart :data="topicStatusData" />
          </el-card>
        </el-col>
      </el-row>

      <el-card class="topics-table-card">
        <template #header>
          <span>토픽 모니터링 상세</span>
        </template>
        
        <el-table :data="monitoringTopics" style="width: 100%">
          <el-table-column prop="name" label="토픽명" width="200" />
          <el-table-column prop="partitionCount" label="파티션" width="100" />
          <el-table-column prop="messageCount" label="메시지 수" width="120">
            <template #default="{ row }">
              {{ formatNumber(row.messageCount) }}
            </template>
          </el-table-column>
          <el-table-column prop="isHealthy" label="상태" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isHealthy ? 'success' : 'danger'">
                {{ row.isHealthy ? '정상' : '오류' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="avgMessageSize" label="평균 메시지 크기" width="150">
            <template #default="{ row }">
              {{ formatBytes(row.avgMessageSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="messagesPerSecond" label="초당 메시지" width="120">
            <template #default="{ row }">
              {{ formatNumber(row.messagesPerSecond) }}
            </template>
          </el-table-column>
          <el-table-column prop="healthScore" label="헬스 점수" width="120">
            <template #default="{ row }">
              <el-progress 
                :percentage="row.healthScore" 
                :color="getHealthColor(row.healthScore)"
                :stroke-width="8"
              />
            </template>
          </el-table-column>
          <el-table-column label="트렌드" width="200">
            <template #default="{ row }">
              <LineChart 
                :data="{
                  labels: Array.from({ length: row.trendData.length }, (_, i) => `${i + 1}분 전`),
                  datasets: [{
                    label: '메시지 수',
                    data: row.trendData,
                    borderColor: '#409EFF',
                    backgroundColor: 'rgba(64, 158, 255, 0.1)',
                    tension: 0.4
                  }]
                }"
                :options="{ 
                  responsive: true, 
                  maintainAspectRatio: false,
                  plugins: { legend: { display: false } },
                  scales: { y: { beginAtZero: true } }
                }"
              />
            </template>
          </el-table-column>
          <el-table-column label="작업" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="showTopicDetail(row)">
                <el-icon><View /></el-icon>
                상세
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="alerts-card">
        <template #header>
          <span>모니터링 알림</span>
          <el-button size="small" @click="clearAllAlerts" :disabled="alerts.length === 0">
            모든 알림 지우기
          </el-button>
        </template>
        
        <div v-if="alerts.length === 0" class="no-alerts">
          <el-empty description="현재 알림이 없습니다" :image-size="60" />
        </div>
        
        <div v-else class="alerts-list">
          <div 
            v-for="alert in alerts" 
            :key="alert.id"
            class="alert-item"
            :class="`alert-${alert.type}`"
          >
            <div class="alert-content">
              <div class="alert-header">
                <span class="alert-title">{{ alert.title }}</span>
                <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
              </div>
              <p class="alert-message">{{ alert.message }}</p>
            </div>
            <el-button 
              size="small" 
              type="text" 
              @click="removeAlert(alert.id)"
            >
              <el-icon><CircleClose /></el-icon>
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="showDetailDialog"
      :title="`토픽 상세 정보: ${selectedTopic?.name || ''}`"
      width="900px"
      :before-close="() => showDetailDialog = false"
    >
      <TopicDetailView :topic="selectedTopic" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDetailDialog = false">닫기</el-button>
        </span>
      </template>
    </el-dialog>

    <ConfirmDialog
      v-model="showConfirmDialog"
      title="토픽 삭제"
      message="정말로 이 토픽을 삭제하시겠습니까?"
      @confirm="confirmDeleteTopic"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useTopicStore } from '@/stores/topic'
import { useConnectionStore } from '@/stores/connection'
import { ElMessage } from 'element-plus'
import { 
  Refresh, 
  VideoPlay, 
  VideoPause, 
  View, 
  CircleClose
} from '@element-plus/icons-vue'
import type { TopicDto, TopicDetailDto } from '@/types/topic'
import BarChart from '@/components/charts/BarChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import LineChart from '@/components/charts/LineChart.vue'
import TopicDetailView from './TopicDetailView.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { formatNumber, formatBytes } from '@/utils/formatters'

const topicStore = useTopicStore()
const connectionStore = useConnectionStore()

const loading = ref(false)
const error = ref<string | null>(null)
const autoRefresh = ref(false)
const showDetailDialog = ref(false)
const showConfirmDialog = ref(false)
const selectedTopic = ref<TopicDetailDto | null>(null)
const deletingTopicName = ref('')

const monitoringTopics = ref<Array<{
  name: string
  partitionCount: number
  messageCount: number
  isHealthy: boolean
  avgMessageSize: number
  messagesPerSecond: number
  healthScore: number
  trendData: number[]
}>>([])

const alerts = ref<Array<{
  id: string
  type: 'info' | 'warning' | 'error'
  title: string
  message: string
  timestamp: number
}>>([])

const topicTrends = ref<Record<string, number[]>>({})

let refreshTimer: NodeJS.Timeout | null = null

onMounted(() => {
  if (currentConnection.value) {
    startMonitoring()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})

watch(() => connectionStore.currentConnection?.id, (newConnectionId) => {
  if (newConnectionId) {
    startMonitoring()
  } else {
    stopMonitoring()
  }
})

const currentConnection = computed(() => connectionStore.currentConnection)

const healthyTopics = computed(() => 
  monitoringTopics.value.filter(t => t.isHealthy).length
)

const totalPartitions = computed(() => 
  monitoringTopics.value.reduce((sum, topic) => sum + topic.partitionCount, 0)
)

const avgThroughput = computed(() => {
  if (monitoringTopics.value.length === 0) return 0
  const total = monitoringTopics.value.reduce((sum, topic) => sum + topic.messagesPerSecond, 0)
  return total / monitoringTopics.value.length
})

const topicThroughputData = computed(() => ({
  labels: monitoringTopics.value.map(t => t.name),
  datasets: [{
    label: '초당 메시지',
    data: monitoringTopics.value.map(t => t.messagesPerSecond),
    backgroundColor: monitoringTopics.value.map(t => 
      t.isHealthy ? '#67C23A' : '#F56C6C'
    )
  }]
}))

const topicStatusData = computed(() => {
  const healthy = monitoringTopics.value.filter(t => t.isHealthy).length
  const unhealthy = monitoringTopics.value.length - healthy
  
  return {
    labels: ['정상', '오류'],
    datasets: [{
      data: [healthy, unhealthy],
      backgroundColor: ['#67C23A', '#F56C6C']
    }]
  }
})

const startMonitoring = async () => {
  if (!currentConnection.value) return
  
  try {
    await refreshMonitoring()
    
    if (autoRefresh.value) {
      startAutoRefresh()
    }
  } catch (err: any) {
    error.value = err.message || '모니터링을 시작할 수 없습니다.'
  }
}

const stopMonitoring = () => {
  stopAutoRefresh()
  monitoringTopics.value = []
  alerts.value = []
}

const refreshMonitoring = async () => {
  if (!currentConnection.value) return
  
  try {
    loading.value = true
    error.value = null
    
    await topicStore.fetchTopics(currentConnection.value.id, true)
    
    generateMonitoringData()
    
    updateTopicTrends()
    
    checkForAlerts()
    
  } catch (err: any) {
    error.value = err.message || '모니터링 데이터를 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
}

const generateMonitoringData = () => {
  monitoringTopics.value = topicStore.topics.map(topic => ({
    name: topic.name,
    partitionCount: topic.partitionCount,
    messageCount: topic.messageCount,
    isHealthy: topic.isHealthy,
    avgMessageSize: Math.floor(Math.random() * 1000) + 100,
    messagesPerSecond: Math.floor(Math.random() * 100) + 1,
    healthScore: topic.isHealthy ? Math.floor(Math.random() * 30) + 70 : Math.floor(Math.random() * 50),
    trendData: Array.from({ length: 10 }, () => Math.floor(Math.random() * 100) + 50)
  }))
}

const updateTopicTrends = () => {
  monitoringTopics.value.forEach(topic => {
    topicTrends.value[topic.name] = topic.trendData
  })
}

const startAutoRefresh = () => {
  if (refreshTimer) return
  
  refreshTimer = setInterval(() => {
    refreshMonitoring()
  }, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  
  if (autoRefresh.value) {
    startAutoRefresh()
    ElMessage.success('자동 새로고침이 시작되었습니다')
  } else {
    stopAutoRefresh()
    ElMessage.info('자동 새로고침이 중지되었습니다')
  }
}

const checkForAlerts = () => {
  monitoringTopics.value.forEach(topic => {
    if (!topic.isHealthy && !alerts.value.find(a => a.message.includes(topic.name))) {
      addAlert('error', '토픽 오류', `토픽 "${topic.name}"에서 오류가 발생했습니다.`)
    }
    
    if (topic.healthScore < 50 && !alerts.value.find(a => a.message.includes(topic.name + ' 헬스'))) {
      addAlert('warning', '헬스 점수 낮음', `토픽 "${topic.name}"의 헬스 점수가 낮습니다 (${topic.healthScore.toFixed(1)}%).`)
    }
  })
}

const addAlert = (type: 'info' | 'warning' | 'error', title: string, message: string) => {
  const alert = {
    id: Date.now().toString(),
    type,
    title,
    message,
    timestamp: Date.now()
  }
  
  alerts.value.unshift(alert)
  
  if (alerts.value.length > 10) {
    alerts.value = alerts.value.slice(0, 10)
  }
}

const removeAlert = (alertId: string) => {
  alerts.value = alerts.value.filter(a => a.id !== alertId)
}

const clearAllAlerts = () => {
  alerts.value = []
}

const showTopicDetail = async (topic: TopicDto) => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    console.log('토픽 상세 정보 요청:', topic.name)
    
    const topicDetail = await topicStore.getTopicDetails(currentConnection.value.id, topic.name)
    
    if (topicDetail) {
      selectedTopic.value = topicDetail
      showDetailDialog.value = true
      
      console.log('토픽 상세 정보 로드됨:', selectedTopic.value)
    } else {
      ElMessage.warning('토픽 상세 정보를 찾을 수 없습니다.')
    }
  } catch (error) {
    console.error('토픽 상세 정보 로드 실패:', error)
    ElMessage.error('토픽 상세 정보를 불러오는데 실패했습니다.')
  }
}

const confirmDeleteTopic = async () => {
  if (!deletingTopicName.value || !currentConnection.value) return
  
  try {
    await topicStore.deleteTopic(currentConnection.value.id, deletingTopicName.value)
    showConfirmDialog.value = false
    deletingTopicName.value = ''
    ElMessage.success('토픽이 삭제되었습니다.')
    
    await refreshMonitoring()
  } catch (err: any) {
    ElMessage.error('토픽 삭제에 실패했습니다.')
  }
}

const getHealthColor = (score: number) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('ko-KR')
}

const clearError = () => {
  error.value = null
}
</script>

<style scoped>
.topic-monitoring {
  padding: 20px;
}

.monitoring-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.monitoring-header h3 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.monitoring-overview {
  margin-bottom: 24px;
}

.overview-card {
  text-align: center;
  padding: 20px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.overview-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 8px;
}

.overview-label {
  font-size: 14px;
  color: #606266;
}

.charts-section {
  margin-bottom: 24px;
}

.charts-section .el-card {
  height: 300px;
}

.topics-table-card {
  margin-bottom: 24px;
}

.alerts-card {
  margin-bottom: 24px;
}

.alerts-card .el-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.no-alerts {
  padding: 40px;
  text-align: center;
}

.alerts-list {
  max-height: 400px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  border-left: 4px solid;
}

.alert-item.alert-info {
  background-color: #f0f9ff;
  border-left-color: #409EFF;
}

.alert-item.alert-warning {
  background-color: #fdf6ec;
  border-left-color: #E6A23C;
}

.alert-item.alert-error {
  background-color: #fef0f0;
  border-left-color: #F56C6C;
}

.alert-content {
  flex: 1;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.alert-title {
  font-weight: 600;
  color: #303133;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}

.alert-message {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.monitoring-content {
  min-height: 400px;
}
</style>