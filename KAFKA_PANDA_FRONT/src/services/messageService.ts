import { apiService } from './api'
import type { MessageDto, SendMessageRequest, MessageSearchCriteria, OffsetType, PaginatedResponse } from '@/types/message'

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
      connectionId,
      topicName,
      partitionNumber,
      offset,
      offsetType,
      limit
    }
    return apiService.get<PaginatedResponse<MessageDto>>('/messages', params) 
  }

  async sendMessage(
    connectionId: string,
    topicName: string,
    request: SendMessageRequest
  ): Promise<void> {
    const url = `/messages?connectionId=${connectionId}&topicName=${topicName}` 
    return apiService.post<void>(url, request)
  }

  async searchMessages(
    connectionId: string,
    criteria: MessageSearchCriteria
  ): Promise<MessageDto[]> {
    const url = `/messages/search?connectionId=${connectionId}`
    return apiService.post<MessageDto[]>(url, criteria)
  }
}

export const messageService = new MessageService()