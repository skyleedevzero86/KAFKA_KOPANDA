import { defineStore } from 'pinia'
import { ref } from 'vue'
import { messageService } from '@/services/messageService'
import type { MessageDto, SendMessageRequest, MessageSearchCriteria, OffsetType } from '@/types/message'

export const useMessageStore = defineStore('message', () => {
  const messages = ref<MessageDto[]>([])
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
    try {
      loading.value = true
      error.value = null
      const response = await messageService.getMessages(
        connectionId,
        topicName,
        partitionNumber,
        offset,
        offsetType,
        limit
      )
      messages.value = response.items
      return response
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch messages'
      console.error('Failed to fetch messages:', err)
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
    try {
      loading.value = true
      error.value = null
      await messageService.sendMessage(connectionId, topicName, request)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to send message'
      console.error('Failed to send message:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function searchMessages(
    connectionId: string,
    criteria: MessageSearchCriteria
  ) {
    try {
      loading.value = true
      error.value = null
      const results = await messageService.searchMessages(connectionId, criteria)
      messages.value = results
      return results
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to search messages'
      console.error('Failed to search messages:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearMessages() {
    messages.value = []
  }

  function clearError() {
    error.value = null
  }

  return {
    messages,
    loading,
    error,
    getMessages,
    sendMessage,
    searchMessages,
    clearMessages,
    clearError
  }
})