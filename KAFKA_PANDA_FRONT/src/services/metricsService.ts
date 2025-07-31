import { apiService } from './api'
import type { KafkaMetricsDto, ConsumerGroupDto } from '@/types/metrics'

export class MetricsService {
  async getMetrics(connectionId: string): Promise<KafkaMetricsDto> {
    try {
      return await apiService.get<KafkaMetricsDto>(`/connections/${connectionId}/metrics`) 
    } catch (error) {
      console.error('Failed to fetch metrics:', error)
      throw error
    }
  }

  async getConsumerGroups(connectionId: string): Promise<ConsumerGroupDto[]> {
    try {
      return await apiService.get<ConsumerGroupDto[]>(`/connections/${connectionId}/consumer-groups`) 
    } catch (error) {
      console.error('Failed to fetch consumer groups:', error)
      throw error
    }
  }

  async createTestConsumerGroup(connectionId: string, topicName: string): Promise<ConsumerGroupDto> {
    try {
      const response = await apiService.post<ConsumerGroupDto>(
        `/connections/${connectionId}/consumer-groups/test?topicName=${topicName}`
      )
      return response
    } catch (error) {
      console.error('Failed to create test consumer group:', error)
      throw error
    }
  }
}

export const metricsService = new MetricsService()