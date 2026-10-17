import { apiService } from './api'
import type { KafkaMetricsDto, ConsumerGroupDto } from '@/types/metrics'

export class MetricsService {
  async getMetrics(connectionId: string): Promise<KafkaMetricsDto> {
    try {
      return await apiService.get<KafkaMetricsDto>(`/connections/${connectionId}/metrics`) 
    } catch (error) {
      console.error('메트릭 조회 실패:', error)
      throw error
    }
  }

  async getConsumerGroups(connectionId: string): Promise<ConsumerGroupDto[]> {
    try {
      return await apiService.get<ConsumerGroupDto[]>(`/connections/${connectionId}/consumer-groups`) 
    } catch (error) {
      console.error('컨슈머 그룹 조회 실패:', error)
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
      console.error('테스트 컨슈머 그룹 생성 실패:', error)
      throw error
    }
  }
}

export const metricsService = new MetricsService()