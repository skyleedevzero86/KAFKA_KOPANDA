import { defineStore } from 'pinia'
import { ref } from 'vue'
import { messageService } from '@/services/messageService'
import type {
  MessageDto,
  SendMessageRequest,
  MessageSearchCriteria,
  PaginatedResponse
} from '@/types/message'
import type { OffsetType } from '@/types/message'

export const useMessageStore = defineStore('message', () => {
  const messages = ref<MessageDto[]>([])
  const pagination = ref<PaginatedResponse<MessageDto> | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function getMessages(
    connectionId: string,
    topicName: string,
    partitionNumber: number,
    offset: number | null,
    offsetType: OffsetType,
    limit: number
  ) {
    loading.value = true
    error.value = null
    try {
      const response = await messageService.getMessages(
        connectionId,
        topicName,
        partitionNumber,
        offset,
        offsetType,
        limit
      )
      messages.value = response.items
      pagination.value = response
      return response
    } catch (err) {
      error.value = err instanceof Error ? err.message : '메시지 목록을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(
    connectionId: string,
    topicName: string,
    request: SendMessageRequest
  ) {
    loading.value = true
    error.value = null
    try {
      await messageService.sendMessage(connectionId, topicName, request)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '메시지 전송에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function searchMessages(
    connectionId: string,
    criteria: MessageSearchCriteria
  ) {
    loading.value = true
    error.value = null
    try {
      messages.value = await messageService.searchMessages(connectionId, criteria)
      return messages.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '메시지 검색에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearMessages() {
    messages.value = []
    pagination.value = null
  }

  function clearError() {
    error.value = null
  }

  return {
    messages,
    pagination,
    loading,
    error,
    getMessages,
    sendMessage,
    searchMessages,
    clearMessages,
    clearError
  }
})