<template>
  <div class="metrics-dashboard">
    <div class="dashboard-header">
      <h3>메트릭스 대시보드</h3>
      <el-button @click="refreshMetrics" :loading="loading">
        <el-icon><Refresh /></el-icon>
        새로고침
      </el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <LoadingSpinner message="메트릭스를 불러오는 중..." />
    </div>

    <div v-else-if="error" class="error-container">
      <ErrorMessage :message="error" @close="clearError" />
    </div>

    <div v-else class="metrics-content">

      <BrokerMetrics :metrics="brokerMetrics" />
      
      <TopicMetrics :topics="topics" />
      
      <ConsumerGroupMetrics :consumer-groups="consumerGroups" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import { useConnectionStore } from '@/stores/connection'
import { useTopicStore } from '@/stores/topic'
import { Refresh } from '@element-plus/icons-vue'
import BrokerMetrics from './BrokerMetrics.vue'
import TopicMetrics from './TopicMetrics.vue'
import ConsumerGroupMetrics from './ConsumerGroupMetrics.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { KafkaMetricsDto, ConsumerGroupDto } from '@/types/metrics'
import type { TopicDto } from '@/types/topic'

const metricsStore = useMetricsStore()
const connectionStore = useConnectionStore()
const topicStore = useTopicStore()

const { loading, error } = metricsStore

const brokerMetrics = computed(() => metricsStore.metrics)
const consumerGroups = computed(() => metricsStore.consumerGroups)
const topics = computed(() => topicStore.topics)

onMounted(async () => {
  if (connectionStore.currentConnection) {
    await refreshMetrics()
  }
})


watch(() => connectionStore.currentConnection?.id, async (newConnectionId) => {
  if (newConnectionId) {
    await refreshMetrics()
  }
})

const refreshMetrics = async () => {
  if (connectionStore.currentConnection) {
    try {

      await Promise.all([
        metricsStore.fetchMetrics(connectionStore.currentConnection.id),
        metricsStore.fetchConsumerGroups(connectionStore.currentConnection.id),
        topicStore.fetchTopics(connectionStore.currentConnection.id)
      ])
    } catch (error) {
      console.error('메트릭 새로고침 실패:', error)
    }
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

.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.metrics-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
}
</style>