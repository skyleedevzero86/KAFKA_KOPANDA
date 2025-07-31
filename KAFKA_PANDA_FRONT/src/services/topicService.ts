import { apiService } from './api'
import type { TopicDto, TopicDetailDto, CreateTopicRequest, DeleteTopicRequest } from '@/types/topic'

export class TopicService {
  async getTopics(connectionId: string): Promise<TopicDto[]> {
    if (!connectionId) {
      throw new Error('Connection ID is required')
    }
    return apiService.get<TopicDto[]>(`/topics?connectionId=${connectionId}`) 
  }

  async getTopicDetails(connectionId: string, topicName: string): Promise<TopicDetailDto> {
    if (!connectionId || !topicName) {
      throw new Error('Connection ID and Topic Name are required')
    }
    return apiService.get<TopicDetailDto>(`/topics/${topicName}?connectionId=${connectionId}`) 
  }

  async createTopic(connectionId: string, request: CreateTopicRequest): Promise<TopicDto> {
    if (!connectionId) {
      throw new Error('Connection ID is required')
    }
    return apiService.post<TopicDto>(`/topics?connectionId=${connectionId}`, request) 
  }

  async deleteTopic(connectionId: string, topicName: string): Promise<void> {
    if (!connectionId || !topicName) {
      throw new Error('Connection ID and Topic Name are required')
    }
    return apiService.delete<void>(`/topics/${topicName}?connectionId=${connectionId}`) 
  }

  async createTestTopic(connectionId: string): Promise<TopicDto> {
    const testRequest: CreateTopicRequest = {
      name: `test-topic-${Date.now()}`,
      partitions: 3,
      replicationFactor: 1,
      config: {}
    }
    return this.createTopic(connectionId, testRequest)
  }
}

export const topicService = new TopicService()