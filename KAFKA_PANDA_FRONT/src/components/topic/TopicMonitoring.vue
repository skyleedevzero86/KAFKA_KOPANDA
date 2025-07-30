<template>
  <div class="topic-monitoring">
    <div class="monitoring-header">
      <h3>토픽 모니터링</h3>
      <div class="header-actions">
        <el-switch
          v-model="autoRefresh"
          active-text="자동 새로고침"
          inactive-text="수동 새로고침"
        />
        <el-select v-model="refreshInterval" :disabled="!autoRefresh">
          <el-option label="5초" :value="5000" />
          <el-option label="10초" :value="10000" />
          <el-option label="30초" :value="30000" />
          <el-option label="1분" :value="60000" />
        </el-select>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          새로고침
        </el-button>
      </div>
    </div>

    <div v-if="!currentConnection" class="no-connection">
      <el-empty description="토픽 모니터링을 위해 연결을 선택하세요" />
    </div>

    <div v-else>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="monitoring-card">
            <template #header>
              <div class="card-header">
                <span>토픽 상태 요약</span>
                <el-icon><DataAnalysis /></el-icon>
              </div>
            </template>
            <div class="summary-stats">
              <div class="stat-item">
                <div class="stat-value">{{ totalTopics }}</div>
                <div class="stat-label">총 토픽</div>
              </div>
              <div class="stat-item">
                <div class="stat-value success">{{ healthyTopics }}</div>
                <div class="stat-label">정상</div>
              </div>
              <div class="stat-item">
                <div class="stat-value danger">{{ unhealthyTopics }}</div>
                <div class="stat-label">오류</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="monitoring-card">
            <template #header>
              <div class="card-header">
                <span>파티션 상태</span>
                <el-icon><Grid /></el-icon>
              </div>
            </template>
            <div class="summary-stats">
              <div class="stat-item">
                <div class="stat-value">{{ totalPartitions }}</div>
                <div class="stat-label">총 파티션</div>
              </div>
              <div class="stat-item">
                <div class="stat-value success">{{ healthyPartitions }}</div>
                <div class="stat-label">정상</div>
              </div>
              <div class="stat-item">
                <div class="stat-value warning">{{ underReplicatedPartitions }}</div>
                <div class="stat-label">복제 부족</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="monitoring-card">
            <template #header>
              <div class="card-header">
                <span>메시지 처리량</span>
                <el-icon><TrendCharts /></el-icon>
              </div>
            </template>
            <div class="summary-stats">
              <div class="stat-item">
                <div class="stat-value">{{ totalMessages }}</div>
                <div class="stat-label">총 메시지</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ messagesPerSecond }}</div>
                <div class="stat-label">초당 메시지</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ avgMessageSize }}</div>
                <div class="stat-label">평균 크기</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>토픽별 메시지 처리량</span>
            </template>
            <div class="chart-container">
              <BarChart
                :data="topicThroughputData"
                :options="barChartOptions"
              />
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card>
            <template #header>
              <span>파티션 상태 분포</span>
            </template>
            <div class="chart-container">
              <PieChart
                :data="partitionStatusData"
                :options="pieChartOptions"
              />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <el-card>
            <template #header>
              <span>토픽 상세 모니터링</span>
            </template>
            <el-table :data="monitoringTopics" style="width: 100%" v-loading="loading">
              <el-table-column prop="name" label="토픽명" width="200" />
              <el-table-column prop="partitionCount" label="파티션" width="100" />
              <el-table-column prop="messageCount" label="메시지 수" width="120">
                <template #default="{ row }">
                  {{ formatNumber(row.messageCount) }}
                </template>
              </el-table-column>
              <el-table-column prop="avgMessageSize" label="평균 크기" width="120">
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
              <el-table-column prop="isHealthy" label="상태" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.isHealthy ? 'success' : 'danger'">
                    {{ row.isHealthy ? '정상' : '오류' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="트렌드" width="200">
                <template #default="{ row }">
                  <LineChart
                    :data="getTopicTrendData(row.name)"
                    :options="miniChartOptions"
                  />
                </template>
              </el-table-column>
              <el-table-column label="작업" width="150">
                <template #default="{ row }">
                  <el-button size="small" @click="viewTopicDetails(row)">
                    상세보기
                  </el-button>
                  <el-button size="small" type="danger" @click="deleteTopic(row)">
                    삭제
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <el-card>
            <template #header>
              <span>실시간 알림</span>
            </template>
            <div class="alerts-container">
              <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.type">
                <div class="alert-icon">
                  <el-icon>
                    <component :is="getAlertIcon(alert.type)" />
                  </el-icon>
                </div>
                <div class="alert-content">
                  <div class="alert-title">{{ alert.title }}</div>
                  <div class="alert-message">{{ alert.message }}</div>
                  <div class="alert-time">{{ formatDate(alert.timestamp) }}</div>
                </div>
                <div class="alert-actions">
                  <el-button size="small" @click="dismissAlert(alert.id)">
                    해제
                  </el-button>
                </div>
              </div>
              <div v-if="alerts.length === 0" class="no-alerts">
                <el-empty description="현재 알림이 없습니다" />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="showTopicDetails" title="토픽 상세 정보" width="800px">
      <TopicDetailView v-if="selectedTopic" :topic="selectedTopic" />
    </el-dialog>

    <ConfirmDialog
      v-model="showDeleteDialog"
      title="토픽 삭제"
      message="정말로 이 토픽을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
      @confirm="confirmDelete"
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
  DataAnalysis, 
  Grid, 
  TrendCharts,
  Warning,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import type { TopicDto } from '@/types/topic'
import BarChart from '@/components/charts/BarChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import LineChart from '@/components/charts/LineChart.vue'
import TopicDetailView from './TopicDetailView.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { formatNumber, formatBytes, formatDate } from '@/utils/formatters'

const topicStore = useTopicStore()
const connectionStore = useConnectionStore()

const autoRefresh = ref(true)
const refreshInterval = ref(10000)
const loading = ref(false)
const showTopicDetails = ref(false)
const showDeleteDialog = ref(false)
const selectedTopic = ref<TopicDto | null>(null)
const deletingTopic = ref<TopicDto | null>(null)

const currentConnection = computed(() => connectionStore.currentConnection)
const { topics, loading: topicLoading, error } = topicStore

interface MonitoringTopic extends TopicDto {
  avgMessageSize: number
  messagesPerSecond: number
  healthScore: number
  trendData: number[]
}

interface Alert {
  id: number
  type: 'success' | 'warning' | 'error'
  title: string
  message: string
  timestamp: Date
}

const monitoringTopics = ref<MonitoringTopic[]>([])
const alerts = ref<Alert[]>([])
const topicTrends = ref<Record<string, number[]>>({})

let refreshTimer: NodeJS.Timeout | null = null

onMounted(() => {
  if (currentConnection.value) {
    loadMonitoringData()
  }
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

watch(currentConnection, (connection) => {
  if (connection) {
    loadMonitoringData()
  }
})

watch(autoRefresh, (enabled) => {
  if (enabled) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
})

watch(refreshInterval, () => {
  if (autoRefresh.value) {
    stopAutoRefresh()
    startAutoRefresh()
  }
})

const totalTopics = computed(() => topics.length)
const healthyTopics = computed(() => topics.filter(t => t.isHealthy).length)
const unhealthyTopics = computed(() => topics.filter(t => !t.isHealthy).length)

const totalPartitions = computed(() => 
  topics.reduce((sum, topic) => sum + topic.partitionCount, 0)
)

const healthyPartitions = computed(() => 
  monitoringTopics.value.filter(t => t.isHealthy).reduce((sum, topic) => sum + topic.partitionCount, 0)
)

const underReplicatedPartitions = computed(() => 
  totalPartitions.value - healthyPartitions.value
)

const totalMessages = computed(() => 
  topics.reduce((sum, topic) => sum + topic.messageCount, 0)
)

const messagesPerSecond = computed(() => 
  monitoringTopics.value.reduce((sum, topic) => sum + topic.messagesPerSecond, 0)
)

const avgMessageSize = computed(() => {
  const totalSize = monitoringTopics.value.reduce((sum, topic) => sum + topic.avgMessageSize, 0)
  return totalSize / monitoringTopics.value.length || 0
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

const partitionStatusData = computed(() => ({
  labels: ['정상', '복제 부족', '오류'],
  datasets: [{
    data: [
      healthyPartitions.value,
      underReplicatedPartitions.value,
      unhealthyTopics.value
    ],
    backgroundColor: ['#67C23A', '#E6A23C', '#F56C6C']
  }]
}))

const barChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  scales: {
    y: {
      beginAtZero: true
    }
  }
}

const pieChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom'
    }
  }
}

const miniChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
    }
  },
  scales: {
    x: {
      display: false
    },
    y: {
      display: false
    }
  }
}

const loadMonitoringData = async () => {
  if (!currentConnection.value) return

  loading.value = true
  try {
    await topicStore.fetchTopics(currentConnection.value.id)
    
    monitoringTopics.value = topics.map(topic => ({
      ...topic,
      avgMessageSize: Math.random() * 1024 + 100,
      messagesPerSecond: Math.random() * 1000 + 10,
      healthScore: topic.isHealthy ? Math.random() * 30 + 70 : Math.random() * 30,
      trendData: generateTrendData()
    }))

    updateTopicTrends()
    checkForAlerts()
  } catch (error) {
    ElMessage.error('모니터링 데이터를 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

const generateTrendData = () => {
  return Array.from({ length: 10 }, () => Math.random() * 100)
}

const updateTopicTrends = () => {
  monitoringTopics.value.forEach(topic => {
    topicTrends.value[topic.name] = topic.trendData
  })
}

const getTopicTrendData = (topicName: string) => {
  const trendData = topicTrends.value[topicName] || []
  return {
    labels: Array.from({ length: trendData.length }, (_, i) => i.toString()),
    datasets: [{
      label: '메시지 수',
      data: trendData,
      borderColor: '#409EFF',
      backgroundColor: 'rgba(64, 158, 255, 0.1)',
      borderWidth: 1,
      fill: false
    }]
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

const addAlert = (type: 'success' | 'warning' | 'error', title: string, message: string) => {
  alerts.value.unshift({
    id: Date.now(),
    type,
    title,
    message,
    timestamp: new Date()
  })
  
  if (alerts.value.length > 50) {
    alerts.value = alerts.value.slice(0, 50)
  }
}

const dismissAlert = (alertId: number) => {
  alerts.value = alerts.value.filter(alert => alert.id !== alertId)
}

const getAlertIcon = (type: string) => {
  switch (type) {
    case 'success': return CircleCheck
    case 'warning': return Warning
    case 'error': return CircleClose
    default: return Warning
  }
}

const getHealthColor = (score: number) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

const refreshData = () => {
  loadMonitoringData()
}

const startAutoRefresh = () => {
  if (autoRefresh.value) {
    refreshTimer = setInterval(() => {
      refreshData()
    }, refreshInterval.value)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const viewTopicDetails = (topic: TopicDto) => {
  selectedTopic.value = topic
  showTopicDetails.value = true
}

const deleteTopic = (topic: TopicDto) => {
  deletingTopic.value = topic
  showDeleteDialog.value = true
}

const confirmDelete = async () => {
  if (!deletingTopic.value || !currentConnection.value) return

  try {
    await topicStore.deleteTopic(currentConnection.value.id, deletingTopic.value.name)
    ElMessage.success('토픽이 성공적으로 삭제되었습니다.')
    showDeleteDialog.value = false
    deletingTopic.value = null
    loadMonitoringData()
  } catch (error) {
    ElMessage.error('토픽 삭제에 실패했습니다.')
  }
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
  margin-bottom: 20px;
}

.monitoring-header h3 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.no-connection {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.monitoring-card {
  height: 200px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 120px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: #409EFF;
}

.stat-value.success {
  color: #67C23A;
}

.stat-value.danger {
  color: #F56C6C;
}

.stat-value.warning {
  color: #E6A23C;
}

.stat-label {
  color: #606266;
  margin-top: 8px;
}

.chart-container {
  height: 300px;
  position: relative;
}

.alerts-container {
  max-height: 400px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  border-left: 4px solid;
}

.alert-item.success {
  background-color: #f0f9ff;
  border-left-color: #67C23A;
}

.alert-item.warning {
  background-color: #fdf6ec;
  border-left-color: #E6A23C;
}

.alert-item.error {
  background-color: #fef0f0;
  border-left-color: #F56C6C;
}

.alert-icon {
  margin-right: 12px;
  font-size: 1.2rem;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.alert-message {
  color: #606266;
  margin-bottom: 4px;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}

.alert-actions {
  margin-left: 12px;
}

.no-alerts {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}
</style>