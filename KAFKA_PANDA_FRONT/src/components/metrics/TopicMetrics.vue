<template>
  <el-card class="topic-metrics">
    <template #header>
      <div class="card-header">
        <span>토픽 메트릭</span>
      </div>
    </template>
    
    <div v-if="!metrics" class="no-data">
      <el-empty description="메트릭 데이터가 없습니다" />
    </div>
    
    <div v-else class="metrics-content">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="metric-item">
            <div class="metric-value">{{ metrics.topicCount }}</div>
            <div class="metric-label">토픽 수</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="metric-item">
            <div class="metric-value">{{ metrics.totalPartitions }}</div>
            <div class="metric-label">총 파티션</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="metric-item">
            <div class="metric-value">{{ formatNumber(metrics.messagesPerSecond) }}</div>
            <div class="metric-label">초당 메시지</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { formatNumber } from '@/utils/formatters'
import type { KafkaMetricsDto } from '@/types/metrics'

interface Props {
  metrics: KafkaMetricsDto | null
}

defineProps<Props>()
</script>

<style scoped>
.topic-metrics {
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