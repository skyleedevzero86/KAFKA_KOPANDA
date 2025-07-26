<template>
  <div class="metrics-dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon size="24"><Monitor /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics?.brokerCount || 0 }}</div>
              <div class="metric-label">브로커 수</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon size="24"><Document /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics?.topicCount || 0 }}</div>
              <div class="metric-label">토픽 수</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon size="24"><Connection /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics?.activeConnections || 0 }}</div>
              <div class="metric-label">활성 연결</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon">
              <el-icon size="24"><TrendCharts /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ formatNumber(metrics?.messagesPerSecond || 0) }}</div>
              <div class="metric-label">초당 메시지</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>처리량 (초당)</span>
            </div>
          </template>
          <LineChart
            :data="throughputData"
            title="메시지 처리량"
            height="300"
          />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>네트워크 사용량</span>
            </div>
          </template>
          <LineChart
            :data="networkData"
            title="바이트 입출력"
            height="300"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>토픽별 메시지 수</span>
            </div>
          </template>
          <PieChart
            :data="topicData"
            title="토픽 분포"
            height="300"
          />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>시스템 상태</span>
            </div>
          </template>
          <div class="status-grid">
            <div class="status-item">
              <GaugeChart
                :value="systemHealth"
                :min="0"
                :max="100"
                title="시스템 건강도"
                height="150"
                color="#67C23A"
              />
            </div>
            <div class="status-item">
              <GaugeChart
                :value="cpuUsage"
                :min="0"
                :max="100"
                title="CPU 사용률"
                height="150"
                color="#E6A23C"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import { ElMessage } from 'element-plus'
import { Monitor, Document, Connection, TrendCharts } from '@element-plus/icons-vue'
import { formatNumber } from '@/utils/formatters'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import GaugeChart from '@/components/charts/GaugeChart.vue'

interface Props {
  connectionId: string
}

const props = defineProps<Props>()

const metricsStore = useMetricsStore()
const refreshInterval = ref<NodeJS.Timeout | null>(null)

const { metrics, loading, error } = metricsStore

// 차트 데이터 생성
const throughputData = computed(() => {
  if (!metrics.value) return []
  
  return [
    { time: new Date().toISOString(), value: metrics.value.messagesPerSecond }
  ]
})

const networkData = computed(() => {
  if (!metrics.value) return []
  
  return [
    { time: new Date().toISOString(), value: metrics.value.bytesInPerSec, category: '입력' },
    { time: new Date().toISOString(), value: metrics.value.bytesOutPerSec, category: '출력' }
  ]
})

const topicData = computed(() => {
  // 실제로는 토픽별 데이터를 가져와야 함
  return [
    { type: '토픽 1', value: 1000 },
    { type: '토픽 2', value: 800 },
    { type: '토픽 3', value: 600 }
  ]
})

const systemHealth = computed(() => {
  if (!metrics.value) return 0
  // 간단한 건강도 계산
  return Math.min(100, Math.max(0, 100 - (metrics.value.activeConnections * 2)))
})

const cpuUsage = computed(() => {
  if (!metrics.value) return 0
  // CPU 사용률 시뮬레이션
  return Math.min(100, Math.max(0, 20 + (metrics.value.messagesPerSecond / 10)))
})

const loadMetrics = async () => {
  try {
    await metricsStore.fetchMetrics(props.connectionId)
  } catch (error) {
    ElMessage.error('메트릭을 불러오는데 실패했습니다.')
  }
}

onMounted(() => {
  loadMetrics()
  
  // 30초마다 메트릭 새로고침
  refreshInterval.value = setInterval(loadMetrics, 30000)
})

onUnmounted(() => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
  }
})
</script>

<style scoped>
.metrics-dashboard {
  padding: 20px;
}

.metric-card {
  height: 120px;
}

.metric-content {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.metric-icon {
  color: #409EFF;
}

.metric-info {
  flex: 1;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.card-header {
  font-weight: bold;
  color: #303133;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.status-item {
  display: flex;
  justify-content: center;
}
</style>