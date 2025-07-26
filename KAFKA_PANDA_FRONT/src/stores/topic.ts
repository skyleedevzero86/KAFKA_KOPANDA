import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { topicService } from '@/services/topicService'
import type {
  TopicDto,
  TopicDetailDto,
  CreateTopicRequest
} from '@/types/topic'

export const useTopicStore = defineStore('topic', () => {
  const topics = ref<TopicDto[]>([])
  const currentTopic = ref<TopicDetailDto | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const currentConnectionId = ref<string | null>(null)

  const healthyTopics = computed(() => 
    topics.value.filter(topic => topic.isHealthy)
  )

  const internalTopics = computed(() => 
    topics.value.filter(topic => topic.isInternal)
  )

  const userTopics = computed(() => 
    topics.value.filter(topic => !topic.isInternal)
  )

  async function fetchTopics(connectionId: string) {
    loading.value = true
    error.value = null
    currentConnectionId.value = connectionId
    try {
      topics.value = await topicService.getTopics(connectionId)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '토픽 목록을 불러오는데 실패했습니다.'
    } finally {
      loading.value = false
    }
  }

  async function getTopicDetails(connectionId: string, topicName: string) {
    loading.value = true
    error.value = null
    try {
      currentTopic.value = await topicService.getTopicDetails(connectionId, topicName)
      return currentTopic.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '토픽 상세 정보를 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createTopic(connectionId: string, request: CreateTopicRequest) {
    loading.value = true
    error.value = null
    try {
      const newTopic = await topicService.createTopic(connectionId, request)
      if (currentConnectionId.value === connectionId) {
        topics.value.push(newTopic)
      }
      return newTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : '토픽 생성에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteTopic(connectionId: string, topicName: string) {
    loading.value = true
    error.value = null
    try {
      await topicService.deleteTopic(connectionId, topicName)
      if (currentConnectionId.value === connectionId) {
        topics.value = topics.value.filter(topic => topic.name !== topicName)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '토픽 삭제에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createTestTopic(connectionId: string) {
    loading.value = true
    error.value = null
    try {
      const testTopic = await topicService.createTestTopic(connectionId)
      if (currentConnectionId.value === connectionId) {
        topics.value.push(testTopic)
      }
      return testTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : '테스트 토픽 생성에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  function setCurrentTopic(topic: TopicDetailDto | null) {
    currentTopic.value = topic
  }

  function clearError() {
    error.value = null
  }

  return {
    topics,
    currentTopic,
    loading,
    error,
    currentConnectionId,
    healthyTopics,
    internalTopics,
    userTopics,
    fetchTopics,
    getTopicDetails,
    createTopic,
    deleteTopic,
    createTestTopic,
    setCurrentTopic,
    clearError
  }
})