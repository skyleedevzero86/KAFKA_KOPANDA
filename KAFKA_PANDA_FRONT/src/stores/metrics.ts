import { defineStore } from 'pinia'
import { ref } from 'vue'
import { metricsService } from '@/services/metricsService'
import type { KafkaMetricsDto, ConsumerGroupDto } from '@/types/metrics'

export const useMetricsStore = defineStore('metrics', () => {
  const metrics = ref<KafkaMetricsDto | null>(null)
  const consumerGroups = ref<ConsumerGroupDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchMetrics(connectionId: string) {
    try {
      loading.value = true
      error.value = null
      metrics.value = await metricsService.getMetrics(connectionId)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch metrics'
      console.error('Failed to fetch metrics:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchConsumerGroups(connectionId: string) {
    try {
      loading.value = true
      error.value = null
      consumerGroups.value = await metricsService.getConsumerGroups(connectionId)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch consumer groups'
      console.error('Failed to fetch consumer groups:', err)
    } finally {
      loading.value = false
    }
  }

  async function createTestConsumerGroup(connectionId: string, topicName: string) {
    try {
      loading.value = true
      error.value = null
      
      const consumerGroup = await metricsService.createTestConsumerGroup(connectionId, topicName)
      
      // 새로 생성된 컨슈머 그룹을 목록에 추가
      consumerGroups.value.push(consumerGroup)
      
      return consumerGroup
    } catch (err) {
      error.value = err instanceof Error ? err.message : '테스트 컨슈머 그룹 생성 실패'
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
    createTestConsumerGroup,
    clearError
  }
})