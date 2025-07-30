<template>
  <div class="metrics-dashboard">
    <div class="dashboard-header">
      <h3>메트릭 대시보드</h3>
      <div class="header-actions">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          새로고침
        </el-button>
        <el-select v-model="selectedConnectionId" @change="loadMetrics">
          <el-option
              v-for="connection in connections"
              :key="connection.id"
              :label="connection.name"
              :value="connection.id"
          />
        </el-select>
      </div>
    </div>

    <div v-if="!selectedConnectionId" class="no-connection">
      <el-empty description="메트릭을 보려면 연결을 선택하세요" />
    </div>

    <div v-else-if="loading" class="loading">
      <LoadingSpinner message="메트릭을 불러오는 중..." />
    </div>

    <div v-else-if="error" class="error">
      <ErrorMessage :error="error" @close="clearError" />
    </div>

    <div v-else-if="metrics" class="metrics-content">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="metric-card">
            <template #header>
              <div class="metric-header">
                <span>브로커 수</span>
                <el-icon><Monitor /></el-icon>
              </div>
            </template>
            <div class="metric-value">{{ metrics.brokerCount }}</div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="metric-card">
            <template #header>
              <div class="metric-header">
                <span>토픽 수</span>
                <el-icon><Document /></el-icon>
              </div>
            </template>
            <div class="metric-value">{{ metrics.topicCount }}</div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="metric-card">
            <template #header>
              <div class="metric-header">
                <span>총 파티션</span>
                <el-icon><Grid /></el-icon>
              </div>
            </template>
            <div class="metric-value">{{ metrics.totalPartitions }}</div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="metric-card">
            <template #header>
              <div class="metric-header">
                <span>활성 연결</span>
                <el-icon><Connection /></el-icon>
              </div>
            </template>
            <div class="metric-value">{{ metrics.activeConnections }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>처리량 (초당 메시지)</span>
            </template>
            <LineChart
                :data="throughputData"
                :options="lineChartOptions"
            />
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card>
            <template #header>
              <span>네트워크 사용량</span>
            </template>
            <LineChart
                :data="networkData"
                :options="lineChartOptions"
            />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <el-card>
            <template #header>
              <span>컨슈머 그룹 상태</span>
            </template>
            <ConsumerGroupMetrics :consumer-groups="consumerGroups" />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import { useConnectionStore } from '@/stores/connection'
import { ElMessage } from 'element-plus'
import { Refresh, Monitor, Document, Grid, Connection } from '@element-plus/icons-vue'
import type { KafkaMetricsDto } from '@/types/metrics'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import LineChart from '@/components/charts/LineChart.vue'
import ConsumerGroupMetrics from './ConsumerGroupMetrics.vue'

const metricsStore = useMetricsStore()
const connectionStore = useConnectionStore()

const selectedConnectionId = ref<string>('')

const { metrics, consumerGroups, loading, error } = metricsStore
const { connections } = connectionStore

onMounted(() => {
  connectionStore.fetchConnections()
})

const throughputData = computed(() => ({
  labels: ['현재'],
  datasets: [{
    label: '초당 메시지',
    data: metrics.value ? [metrics.value.messagesPerSecond] : [0],
    borderColor: '#409EFF',
    backgroundColor: 'rgba(64, 158, 255, 0.1)'
  }]
}))

const networkData = computed(() => ({
  labels: ['현재'],
  datasets: [
    {
      label: '입력 (MB/s)',
      data: metrics.value ? [metrics.value.bytesInPerSec / 1024 / 1024] : [0],
      borderColor: '#67C23A',
      backgroundColor: 'rgba(103, 194, 58, 0.1)'
    },
    {
      label: '출력 (MB/s)',
      data: metrics.value ? [metrics.value.bytesOutPerSec / 1024 / 1024] : [0],
      borderColor: '#E6A23C',
      backgroundColor: 'rgba(230, 162, 60, 0.1)'
    }
  ]
}))

const lineChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  scales: {
    y: {
      beginAtZero: true
    }
  }
}

const handleRefresh = () => {
  if (selectedConnectionId.value) {
    loadMetrics()
  }
}

const loadMetrics = async () => {
  if (!selectedConnectionId.value) return

  try {
    await Promise.all([
      metricsStore.fetchMetrics(selectedConnectionId.value),
      metricsStore.fetchConsumerGroups(selectedConnectionId.value)
    ])
  } catch (error) {
    ElMessage.error('메트릭을 불러오는데 실패했습니다.')
  }
}

const clearError = () => {
  metricsStore.clearError()
}
</script>

<style scoped>
.metrics-dashboard {
  padding: 20px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h3 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.no-connection,
.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.metric-card {
  text-align: center;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-value {
  font-size: 2rem;
  font-weight: bold;
  color: #409EFF;
}
</style>