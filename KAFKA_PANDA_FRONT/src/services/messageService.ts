import { apiService } from './api'
import type {
  MessageDto,
  SendMessageRequest,
  MessageSearchCriteria,
  PaginatedResponse
} from '@/types/message'
import type { OffsetType } from '@/types/message'

export class MessageService {
  async getMessages(
    connectionId: string,
    topicName: string,
    partitionNumber: number,
    offset: number | null,
    offsetType: OffsetType,
    limit: number
  ): Promise<PaginatedResponse<MessageDto>> {
    const params = {
      offset,
      offsetType,
      limit
    }
    return apiService.get<PaginatedResponse<MessageDto>>(
      `/connections/${connectionId}/topics/${topicName}/partitions/${partitionNumber}/messages`,
      params
    )
  }

  async sendMessage(
    connectionId: string,
    topicName: string,
    request: SendMessageRequest
  ): Promise<void> {
    return apiService.post<void>(
      `/connections/${connectionId}/topics/${topicName}/messages`,
      request
    )
  }

  async searchMessages(
    connectionId: string,
    criteria: MessageSearchCriteria
  ): Promise<MessageDto[]> {
    return apiService.post<MessageDto[]>(
      `/connections/${connectionId}/topics/${criteria.topic}/messages/search`,
      criteria
    )
  }
}

export const messageService = new MessageService()