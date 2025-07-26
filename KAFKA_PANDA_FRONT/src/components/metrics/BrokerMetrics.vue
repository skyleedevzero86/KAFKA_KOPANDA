<template>
  <el-card class="broker-metrics">
    <template #header>
      <div class="card-header">
        <span>브로커 메트릭</span>
      </div>
    </template>
    
    <div v-if="!metrics" class="no-data">
      <el-empty description="메트릭 데이터가 없습니다" />
    </div>
    
    <div v-else class="metrics-content">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="metric-item">
            <div class="metric-value">{{ metrics.brokerCount }}</div>
            <div class="metric-label">브로커 수</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="metric-item">
            <div class="metric-value">{{ formatBytes(metrics.bytesInPerSec) }}/s</div>
            <div class="metric-label">입력 처리량</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="metric-item">
            <div class="metric-value">{{ formatBytes(metrics.bytesOutPerSec) }}/s</div>
            <div class="metric-label">출력 처리량</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { formatBytes } from '@/utils/formatters'
import type { KafkaMetricsDto } from '@/types/metrics'

interface Props {
  metrics: KafkaMetricsDto | null
}

defineProps<Props>()
</script>

<style scoped>
.broker-metrics {
  height: 100%;
}

.card-header {
  font-weight: bold;
  color: #303133;
}

.no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.metrics-content {
  padding: 20px 0;
}

.metric-item {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 14px;
  color: #909399;
}
</style>