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

    <div v-if="!currentConnection" class="no-connection">
      <el-empty description="연결을 선택해주세요">
        <el-button type="primary" @click="$router.push('/connections')">
          연결 관리로 이동
        </el-button>
      </el-empty>
    </div>

    <div v-else-if="loading" class="loading-container">
      <LoadingSpinner message="모니터링 데이터를 불러오는 중..." />
    </div>

    <div v-else-if="error" class="error-container">
      <ErrorMessage :message="error" @close="clearError" />
    </div>

    <div v-else-if="monitoringTopics.length === 0" class="no-topics">
      <el-empty description="모니터링할 토픽이 없습니다">
        <el-button type="primary" @click="refreshMonitoring">
          새로고침
        </el-button>
      </el-empty>
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
        <AlertList />
      </el-card>
    </div>

    <el-dialog
      v-model="showDetailDialog"
      :title="`토픽 상세 정보: ${selectedTopic?.name || ''}`"
      width="900px"
      :before-close="() => showDetailDialog = false"
    >
      <TopicDetailView v-if="selectedTopic" :topic="selectedTopic" />
      <div v-else class="no-topic-selected">
        <el-empty description="선택된 토픽이 없습니다." />
      </div>
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
  View
} from '@element-plus/icons-vue'
import type { TopicDetailDto } from '@/types/topic'
import BarChart from '@/components/charts/BarChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import LineChart from '@/components/charts/LineChart.vue'
import TopicDetailView from './TopicDetailView.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import AlertList from '@/components/common/AlertList.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
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

interface MonitoringTopic {
  name: string
  partitionCount: number
  messageCount: number
  isHealthy: boolean
  avgMessageSize: number
  messagesPerSecond: number
  healthScore: number
  trendData: number[]
}

const monitoringTopics = ref<MonitoringTopic[]>([])


const topicTrends = ref<Record<string, number[]>>({})

let refreshTimer: NodeJS.Timeout | null = null

onMounted(() => {
  console.log('TopicMonitoring 컴포넌트 마운트됨')
  console.log('현재 연결:', currentConnection.value)
  
  if (currentConnection.value) {
    console.log('연결이 있음, 모니터링 시작')
    startMonitoring()
  } else {
    console.log('연결이 없음, 연결을 기다림')
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})

watch(() => connectionStore.currentConnection?.id, (newConnectionId, oldConnectionId) => {
  console.log('연결 변경 감지:', { oldConnectionId, newConnectionId })
  
  if (newConnectionId) {
    console.log('새 연결로 모니터링 시작')
    startMonitoring()
  } else {
    console.log('연결이 없어서 모니터링 중지')
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
  if (!currentConnection.value) {
    console.log('연결이 선택되지 않음')
    return
  }
  
  try {
    console.log('모니터링 시작:', currentConnection.value.name)
    await refreshMonitoring()
    
    if (autoRefresh.value) {
      startAutoRefresh()
    }
  } catch (err: any) {
    console.error('모니터링 시작 실패:', err)
    error.value = err.message || '모니터링을 시작할 수 없습니다.'
  }
}

const stopMonitoring = () => {
  stopAutoRefresh()
  monitoringTopics.value = []
}

const refreshMonitoring = async () => {
  if (!currentConnection.value) {
    console.log('연결이 선택되지 않아 모니터링 데이터를 불러올 수 없습니다')
    return
  }
  
  try {
    loading.value = true
    error.value = null
    
    console.log('토픽 데이터 로드 시작:', currentConnection.value.id)
    await topicStore.fetchTopics(currentConnection.value.id, true)
    
    console.log('로드된 토픽 수:', topicStore.topics.length)
    console.log('토픽 목록:', topicStore.topics.map(t => t.name))
    
    generateMonitoringData()
    
    updateTopicTrends()
    
    console.log('모니터링 데이터 생성 완료:', monitoringTopics.value.length)
    
  } catch (err: any) {
    console.error('모니터링 데이터 로드 실패:', err)
    error.value = err.message || '모니터링 데이터를 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
}

const generateMonitoringData = () => {
  console.log('모니터링 데이터 생성 시작, 토픽 수:', topicStore.topics.length)
  
  if (topicStore.topics.length === 0) {
    console.log('토픽이 없어서 모니터링 데이터를 생성할 수 없습니다')
    monitoringTopics.value = []
    return
  }
  
  monitoringTopics.value = topicStore.topics.map(topic => {
    console.log('토픽 처리 중:', topic.name, '메시지 수:', topic.messageCount)
    return {
      name: topic.name,
      partitionCount: topic.partitionCount,
      messageCount: topic.messageCount,
      isHealthy: topic.isHealthy,
      avgMessageSize: Math.floor(Math.random() * 1000) + 100,
      messagesPerSecond: Math.floor(Math.random() * 100) + 1,
      healthScore: topic.isHealthy ? Math.floor(Math.random() * 30) + 70 : Math.floor(Math.random() * 50),
      trendData: Array.from({ length: 10 }, () => Math.floor(Math.random() * 100) + 50)
    }
  }) as MonitoringTopic[]
  
  console.log('생성된 모니터링 데이터:', monitoringTopics.value.length)
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


const showTopicDetail = async (topic: { name: string }) => {
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

.no-connection,
.no-topics,
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

.no-topic-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}
</style>