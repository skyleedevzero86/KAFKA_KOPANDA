<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2>대시보드</h2>
      <p>Kafka 클러스터 현황을 한눈에 확인합니다.</p>
    </div>

    <div class="dashboard-grid">
      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <el-icon><Connection /></el-icon>
            <span>연결 상태</span>
          </div>
        </template>
        <div class="status-content">
          <div class="status-item">
            <span class="label">활성 연결:</span>
            <span class="value">{{ connectionStore.connections.length }}</span>
          </div>
          <div class="status-item">
            <span class="label">연결된 클러스터:</span>
            <span class="value">{{ connectedClusters }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <el-icon><Document /></el-icon>
            <span>토픽 현황</span>
          </div>
        </template>
        <div class="status-content">
          <div class="status-item">
            <span class="label">총 토픽:</span>
            <span class="value">{{ totalTopics }}</span>
          </div>
          <div class="status-item">
            <span class="label">내부 토픽:</span>
            <span class="value">{{ internalTopics }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <el-icon><Monitor /></el-icon>
            <span>시스템 메트릭스</span>
          </div>
        </template>
        <div class="status-content">
          <div class="status-item">
            <span class="label">브로커 수:</span>
            <span class="value">{{ brokerCount }}</span>
          </div>
          <div class="status-item">
            <span class="label">총 파티션:</span>
            <span class="value">{{ totalPartitions }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <el-icon><PieChartIcon /></el-icon>
                <span>토픽 분포</span>
              </div>
            </template>
            <div v-if="hasTopicData" style="height: 300px;">
              <PieChartComponent 
                :data="topicDistributionData"
                :options="pieChartOptions"
              />
            </div>
            <div v-else class="no-data">
              <el-empty description="토픽 데이터가 없습니다." />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <el-icon><TrendCharts /></el-icon>
                <span>연결 상태</span>
              </div>
            </template>
            <div class="connection-status-list">
              <div 
                v-for="connection in connectionStore.connections" 
                :key="connection.id"
                class="connection-status-item"
              >
                <span class="connection-name">{{ connection.name }}</span>
                <el-tag 
                  :type="getConnectionStatusType(connection)"
                  size="small"
                >
                  {{ getConnectionStatusText(connection) }}
                </el-tag>
              </div>
              <div v-if="connectionStore.connections.length === 0" class="no-connections">
                <el-empty description="연결된 클러스터가 없습니다." />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import { useTopicStore } from '@/stores/topic'
import { useMetricsStore } from '@/stores/metrics'
import { Connection, Document, Monitor, PieChart as PieChartIcon, TrendCharts } from '@element-plus/icons-vue'
import PieChartComponent from '@/components/charts/PieChart.vue'
import { formatDate } from '@/utils/formatters'

const connectionStore = useConnectionStore()
const topicStore = useTopicStore()
const metricsStore = useMetricsStore()

const connectedClusters = computed(() => 
  connectionStore.connections.filter(c => c.lastConnected).length
)

const totalTopics = computed(() => topicStore.topics.length)
const internalTopics = computed(() => 
  topicStore.topics.filter(t => t.isInternal).length
)

const brokerCount = computed(() => metricsStore.brokerCount)
const totalPartitions = computed(() => 
  topicStore.topics.reduce((sum, topic) => sum + topic.partitionCount, 0)
)

const hasTopicData = computed(() => {
  const userTopics = totalTopics.value - internalTopics.value
  const internalTopicsCount = internalTopics.value
  return userTopics > 0 || internalTopicsCount > 0
})

const topicDistributionData = computed(() => {
  const userTopics = totalTopics.value - internalTopics.value
  const internalTopicsCount = internalTopics.value
  
  if (userTopics === 0 && internalTopicsCount === 0) {
    return {
      labels: ['데이터 없음'],
      datasets: [{
        data: [1],
        backgroundColor: ['#f0f0f0'],
        borderColor: ['#d0d0d0'],
        borderWidth: 1
      }]
    }
  }
  
  return {
    labels: ['사용자 토픽', '내부 토픽'],
    datasets: [{
      data: [userTopics, internalTopicsCount],
      backgroundColor: ['#409EFF', '#909399'],
      borderColor: ['#409EFF', '#909399'],
      borderWidth: 1
    }]
  }
})

const pieChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom'
    }
  }
}

const getConnectionStatusType = (connection: any) => {
  if (connection.lastConnected) return 'success'
  return 'danger'
}

const getConnectionStatusText = (connection: any) => {
  if (connection.lastConnected) return '연결됨'
  return '연결 안됨'
}

onMounted(async () => {
  console.log('Dashboard 컴포넌트 마운트됨')
  await connectionStore.fetchConnections()
  if (connectionStore.currentConnection) {
    await topicStore.fetchTopics(connectionStore.currentConnection.id)
    await metricsStore.fetchMetrics(connectionStore.currentConnection.id)
  }
})

onActivated(async () => {
  console.log('Dashboard 컴포넌트 활성화됨')
  await connectionStore.fetchConnections()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #606266;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.status-card {
  height: 200px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-content {
  padding: 16px 0;
}

.status-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.status-item .label {
  color: #606266;
}

.status-item .value {
  font-weight: bold;
  color: #303133;
}

.charts-section {
  margin-top: 24px;
}

.connection-status-list {
  max-height: 300px;
  overflow-y: auto;
}

.connection-status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.connection-status-item:last-child {
  border-bottom: none;
}

.connection-name {
  font-weight: 500;
  color: #303133;
}

.no-connections {
  padding: 20px;
}

.no-data {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>