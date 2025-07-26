import { defineStore } from 'pinia'
import { ref } from 'vue'
import { metricsService } from '@/services/metricsService'
import type {
  KafkaMetricsDto,
  ConsumerGroupDto
} from '@/types/metrics'

export const useMetricsStore = defineStore('metrics', () => {
  const metrics = ref<KafkaMetricsDto | null>(null)
  const consumerGroups = ref<ConsumerGroupDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchMetrics(connectionId: string) {
    loading.value = true
    error.value = null
    try {
      metrics.value = await metricsService.getMetrics(connectionId)
      return metrics.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '메트릭을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchConsumerGroups(connectionId: string) {
    loading.value = true
    error.value = null
    try {
      consumerGroups.value = await metricsService.getConsumerGroups(connectionId)
      return consumerGroups.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '컨슈머 그룹을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    metrics,
    consumerGroups,
    loading,
    error,
    fetchMetrics,
    fetchConsumerGroups,
    clearError
  }
})