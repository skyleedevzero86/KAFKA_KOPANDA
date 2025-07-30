<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h2>대시보드</h2>
      <p>Kafka 클러스터 상태 및 메트릭을 한눈에 확인하세요</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="overview-card">
          <template #header>
            <div class="card-header">
              <span>연결 상태</span>
              <el-icon><Connection /></el-icon>
            </div>
          </template>
          <div class="overview-content">
            <div class="overview-item">
              <span class="label">총 연결:</span>
              <span class="value">{{ connections.length }}</span>
            </div>
            <div class="overview-item">
              <span class="label">연결됨:</span>
              <span class="value success">{{ connectedConnections.length }}</span>
            </div>
            <div class="overview-item">
              <span class="label">연결 안됨:</span>
              <span class="value danger">{{ disconnectedConnections.length }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="overview-card">
          <template #header>
            <div class="card-header">
              <span>토픽 상태</span>
              <el-icon><Document /></el-icon>
            </div>
          </template>
          <div class="overview-content">
            <div class="overview-item">
              <span class="label">총 토픽:</span>
              <span class="value">{{ totalTopics }}</span>
            </div>
            <div class="overview-item">
              <span class="label">정상:</span>
              <span class="value success">{{ healthyTopics }}</span>
            </div>
            <div class="overview-item">
              <span class="label">오류:</span>
              <span class="value danger">{{ unhealthyTopics }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="overview-card">
          <template #header>
            <div class="card-header">
              <span>시스템 상태</span>
              <el-icon><Monitor /></el-icon>
            </div>
          </template>
          <div class="overview-content">
            <div class="overview-item">
              <span class="label">메모리 사용량:</span>
              <span class="value">{{ memoryUsage }}%</span>
            </div>
            <div class="overview-item">
              <span class="label">CPU 사용량:</span>
              <span class="value">{{ cpuUsage }}%</span>
            </div>
            <div class="overview-item">
              <span class="label">디스크 사용량:</span>
              <span class="value">{{ diskUsage }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>연결 상태 분포</span>
          </template>
          <div class="chart-container">
            <PieChart
                :data="connectionChartData"
                :options="pieChartOptions"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>토픽 상태 분포</span>
          </template>
          <div class="chart-container">
            <PieChart
                :data="topicChartData"
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
            <span>최근 활동</span>
          </template>
          <div class="recent-activity">
            <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
              <div class="activity-icon">
                <el-icon :color="activity.color">
                  <component :is="activity.icon" />
                </el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-title">{{ activity.title }}</div>
                <div class="activity-time">{{ formatDate(activity.time) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import { useTopicStore } from '@/stores/topic'
import { Connection, Document, Monitor } from '@element-plus/icons-vue'
import PieChart from '@/components/charts/PieChart.vue'
import { formatDate } from '@/utils/formatters'

const connectionStore = useConnectionStore()
const topicStore = useTopicStore()

const { connections, connectedConnections, disconnectedConnections } = connectionStore
const { topics } = topicStore

// 시스템 메트릭 (실제로는 API에서 가져와야 함)
const memoryUsage = ref(65)
const cpuUsage = ref(45)
const diskUsage = ref(78)

const totalTopics = computed(() => topics.length)
const healthyTopics = computed(() => topics.filter(t => t.isHealthy).length)
const unhealthyTopics = computed(() => topics.filter(t => !t.isHealthy).length)

const connectionChartData = computed(() => ({
  labels: ['연결됨', '연결 안됨'],
  datasets: [{
    data: [connectedConnections.length, disconnectedConnections.length],
    backgroundColor: ['#67C23A', '#F56C6C']
  }]
}))

const topicChartData = computed(() => ({
  labels: ['정상', '오류'],
  datasets: [{
    data: [healthyTopics.value, unhealthyTopics.value],
    backgroundColor: ['#67C23A', '#F56C6C']
  }]
}))

const pieChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom'
    }
  }
}

const recentActivities = ref([
  {
    id: 1,
    title: '새 연결이 생성되었습니다: Local Kafka',
    time: new Date(Date.now() - 1000 * 60 * 5),
    icon: 'Connection',
    color: '#409EFF'
  },
  {
    id: 2,
    title: '토픽 "test-topic"이 생성되었습니다',
    time: new Date(Date.now() - 1000 * 60 * 15),
    icon: 'Document',
    color: '#67C23A'
  },
  {
    id: 3,
    title: '연결 "Production Kafka"가 오프라인 상태가 되었습니다',
    time: new Date(Date.now() - 1000 * 60 * 30),
    icon: 'Warning',
    color: '#E6A23C'
  }
])

const loadData = () => {
  console.log('Dashboard 데이터 로드')
  connectionStore.fetchConnections()
  topicStore.fetchTopics('')
}

onMounted(() => {
  console.log('Dashboard 컴포넌트 마운트됨')
  loadData()
})

onActivated(() => {
  console.log('Dashboard 컴포넌트 활성화됨')
  loadData()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.dashboard-header {
  margin-bottom: 30px;
  text-align: center;
}

.dashboard-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.dashboard-header p {
  margin: 0;
  color: #606266;
}

.overview-card {
  height: 200px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.overview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-item .label {
  color: #606266;
}

.overview-item .value {
  font-weight: bold;
  font-size: 1.2rem;
}

.overview-item .value.success {
  color: #67C23A;
}

.overview-item .value.danger {
  color: #F56C6C;
}

.chart-container {
  height: 300px;
  position: relative;
}

.recent-activity {
  max-height: 300px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  margin-right: 12px;
  font-size: 1.2rem;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: #909399;
}
</style>