import { apiService } from './api'
import type {
  KafkaMetricsDto,
  ConsumerGroupDto
} from '@/types/metrics'

export class MetricsService {
  async getMetrics(connectionId: string): Promise<KafkaMetricsDto> {
    return apiService.get<KafkaMetricsDto>(`/connections/${connectionId}/metrics`)
  }

  async getConsumerGroups(connectionId: string): Promise<ConsumerGroupDto[]> {
    return apiService.get<ConsumerGroupDto[]>(`/connections/${connectionId}/consumer-groups`)
  }
}

export const metricsService = new MetricsService()