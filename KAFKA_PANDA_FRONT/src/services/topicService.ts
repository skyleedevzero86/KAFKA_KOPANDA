import { apiService } from './api'
import type {
  TopicDto,
  TopicDetailDto,
  CreateTopicRequest,
  DeleteTopicRequest
} from '@/types/topic'

export class TopicService {
  async getTopics(connectionId: string): Promise<TopicDto[]> {
    return apiService.get<TopicDto[]>(`/connections/${connectionId}/topics`)
  }

  async getTopicDetails(connectionId: string, topicName: string): Promise<TopicDetailDto> {
    return apiService.get<TopicDetailDto>(`/connections/${connectionId}/topics/${topicName}`)
  }

  async createTopic(connectionId: string, request: CreateTopicRequest): Promise<TopicDto> {
    return apiService.post<TopicDto>(`/connections/${connectionId}/topics`, request)
  }

  async deleteTopic(connectionId: string, topicName: string): Promise<void> {
    return apiService.delete<void>(`/connections/${connectionId}/topics/${topicName}`)
  }

  async createTestTopic(connectionId: string): Promise<TopicDto> {
    return apiService.post<TopicDto>(`/test/create-test-topic/${connectionId}`)
  }
}

export const topicService = new TopicService()