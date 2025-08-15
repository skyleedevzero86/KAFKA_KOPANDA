import { defineStore } from 'pinia'
import { ref } from 'vue'
import { topicService } from '@/services/topicService'
import type { TopicDto, TopicDetailDto, CreateTopicRequest } from '@/types/topic'

export const useTopicStore = defineStore('topic', () => {
  const topics = ref<TopicDto[]>([])
  const currentTopic = ref<TopicDetailDto | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchTopics(connectionId: string, includeInternal: boolean = true) {
    if (!connectionId) {
      error.value = 'Connection ID is required'
      return
    }

    try {
      loading.value = true
      error.value = null
      topics.value = await topicService.getTopics(connectionId, includeInternal)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch topics'
      console.error('Failed to fetch topics:', err)
    } finally {
      loading.value = false
    }
  }

  async function getTopicDetails(connectionId: string, topicName: string) {
    if (!connectionId || !topicName) {
      error.value = 'Connection ID and Topic Name are required'
      return
    }

    try {
      loading.value = true
      error.value = null
      currentTopic.value = await topicService.getTopicDetails(connectionId, topicName)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch topic details'
      console.error('Failed to fetch topic details:', err)
    } finally {
      loading.value = false
    }
  }

  async function createTopic(connectionId: string, request: CreateTopicRequest) {
  if (!connectionId) {
    error.value = 'Connection ID is required'
    console.error('토픽 생성 실패: connectionId가 없습니다')
    return
  }

  try {
    loading.value = true
    error.value = null
    console.log('토픽 생성 시작:', { connectionId, request })
    
    const newTopic = await topicService.createTopic(connectionId, request)
    console.log('토픽 생성 성공:', newTopic)
    
    topics.value.push(newTopic)
    return newTopic
  } catch (err) {
    console.error('토픽 생성 실패 상세:', err)
    error.value = err instanceof Error ? err.message : 'Failed to create topic'
    console.error('Failed to create topic:', err)
    throw err
  } finally {
    loading.value = false
  }
}

  async function deleteTopic(connectionId: string, topicName: string) {
    if (!connectionId || !topicName) {
      error.value = 'Connection ID and Topic Name are required'
      return
    }

    try {
      loading.value = true
      error.value = null
      await topicService.deleteTopic(connectionId, topicName)
      topics.value = topics.value.filter(topic => topic.name !== topicName)
      if (currentTopic.value?.name === topicName) {
        currentTopic.value = null
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to delete topic'
      console.error('Failed to delete topic:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createTestTopic(connectionId: string) {
    if (!connectionId) {
      error.value = 'Connection ID is required'
      return
    }

    try {
      loading.value = true
      error.value = null
      const newTopic = await topicService.createTestTopic(connectionId)
      topics.value.push(newTopic)
      return newTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to create test topic'
      console.error('Failed to create test topic:', err)
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
    fetchTopics,
    getTopicDetails,
    createTopic,
    deleteTopic,
    createTestTopic,
    setCurrentTopic,
    clearError
  }
})