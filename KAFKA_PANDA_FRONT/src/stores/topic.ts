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
    try {
      loading.value = true
      error.value = null
      const fetchedTopics = await topicService.getTopics(connectionId, includeInternal)
      topics.value = fetchedTopics
    } catch (err: any) {
      error.value = err.message || '토픽 목록을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getTopicDetails(connectionId: string, topicName: string) {
    try {
      loading.value = true
      error.value = null
      const topicDetails = await topicService.getTopicDetails(connectionId, topicName)
      currentTopic.value = topicDetails
      return topicDetails
    } catch (err: any) {
      error.value = err.message || '토픽 상세 정보를 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createTopic(connectionId: string, request: CreateTopicRequest) {
    try {
      loading.value = true
      error.value = null
      
      if (request.name.startsWith('__')) {
        const isInternalTopic = request.name === '__consumer_offsets' || 
                                request.name === '__transaction_state' || 
                                request.name === '__schema_registry'
        
        if (isInternalTopic) {
          throw new Error(`'${request.name}'는 Kafka에서 자동으로 관리되는 내부 토픽입니다. 수동 생성은 권장되지 않습니다.`)
        }
      }
      
      const newTopic = await topicService.createTopic(connectionId, request)
      topics.value.push(newTopic)
      return newTopic
    } catch (err: any) {
      error.value = err.message || '토픽 생성에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteTopic(connectionId: string, topicName: string) {
    try {
      loading.value = true
      error.value = null
      
      if (topicName.startsWith('__')) {
        throw new Error(`'${topicName}'는 Kafka의 내부 토픽으로, 삭제할 수 없습니다.`)
      }
      
      await topicService.deleteTopic(connectionId, topicName)
      topics.value = topics.value.filter(t => t.name !== topicName)
      
      if (currentTopic.value?.name === topicName) {
        currentTopic.value = null
      }
    } catch (err: any) {
      error.value = err.message || '토픽 삭제에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createTestTopic(connectionId: string) {
    try {
      loading.value = true
      error.value = null
      
      const testTopicRequest: CreateTopicRequest = {
        name: `test-topic-${Date.now()}`,
        partitions: 3,
        replicationFactor: 1,
        config: {
          'cleanup.policy': 'delete',
          'retention.ms': '604800000'
        }
      }
      
      const newTopic = await topicService.createTopic(connectionId, testTopicRequest)
      topics.value.push(newTopic)
      return newTopic
    } catch (err: any) {
      error.value = err.message || '테스트 토픽 생성에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function checkInternalTopics(connectionId: string): Promise<string[]> {
    try {
      loading.value = true
      error.value = null
      
      const allTopics = await topicService.getTopics(connectionId, true)
      const internalTopics = allTopics
        .filter(topic => topic.isInternal)
        .map(topic => topic.name)
      
      return internalTopics
    } catch (err: any) {
      error.value = err.message || '내부 토픽 확인에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getInternalTopicsStatus(connectionId: string): Promise<{
    found: string[]
    missing: string[]
    total: number
  }> {
    try {
      const foundTopics = await checkInternalTopics(connectionId)
      
      const expectedInternalTopics = [
        '__consumer_offsets',
        '__transaction_state',
        '__schema_registry'
      ]
      
      const missingTopics = expectedInternalTopics.filter(
        expected => !foundTopics.includes(expected)
      )
      
      return {
        found: foundTopics,
        missing: missingTopics,
        total: foundTopics.length
      }
    } catch (err: any) {
      error.value = err.message || '내부 토픽 상태 확인에 실패했습니다.'
      throw err
    }
  }

  function setCurrentTopic(topic: TopicDetailDto | null) {
    currentTopic.value = topic
  }

  function clearError() {
    error.value = null
  }

  function clearTopics() {
    topics.value = []
    currentTopic.value = null
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
    checkInternalTopics,
    getInternalTopicsStatus,
    setCurrentTopic,
    clearError,
    clearTopics
  }
})