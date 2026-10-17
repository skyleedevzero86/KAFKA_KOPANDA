import { defineStore } from 'pinia'
import { ref } from 'vue'
import { connectionService } from '@/services/connectionService'
import type { ConnectionDto, CreateConnectionRequest, UpdateConnectionRequest, ConnectionTestResult, ConnectionStatus } from '@/types/connection'
import { ConnectionStatusType } from '@/types/connection'

export const useConnectionStore = defineStore('connection', () => {
  const connections = ref<ConnectionDto[]>([])
  const currentConnection = ref<ConnectionDto | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchConnections() {
    try {
      loading.value = true
      error.value = null
      connections.value = await connectionService.getConnections()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch connections'
      console.error('연결 목록 조회 실패:', err)
    } finally {
      loading.value = false
    }
  }

  async function createConnection(request: CreateConnectionRequest) {
    try {
      loading.value = true
      error.value = null
      const newConnection = await connectionService.createConnection(request)
      connections.value.push(newConnection)
      return newConnection
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to create connection'
      console.error('연결 생성 실패:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateConnection(id: string, request: UpdateConnectionRequest) {
    try {
      loading.value = true
      error.value = null
      const updatedConnection = await connectionService.updateConnection(id, request)
      const index = connections.value.findIndex(c => c.id === id)
      if (index !== -1) {
        connections.value[index] = updatedConnection
      }
      if (currentConnection.value?.id === id) {
        currentConnection.value = updatedConnection
      }
      return updatedConnection
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to update connection'
      console.error('연결 수정 실패:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteConnection(id: string) {
    try {
      loading.value = true
      error.value = null
      await connectionService.deleteConnection(id)
      connections.value = connections.value.filter(c => c.id !== id)
      if (currentConnection.value?.id === id) {
        currentConnection.value = null
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to delete connection'
      console.error('연결 삭제 실패:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function testConnection(request: CreateConnectionRequest): Promise<ConnectionTestResult> {
    try {
      loading.value = true
      error.value = null
      return await connectionService.testConnection(request)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to test connection'
      console.error('연결 테스트 실패:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function judgeConnections() {
    await Promise.all(connections.value.map(async (connection) => {
      try {
        const status = await connectionService.getConnectionStatus(connection.id)
        connection.lastConnected = status.status === ConnectionStatusType.CONNECTED
          ? status.lastChecked
          : undefined
      } catch {
        connection.lastConnected = undefined
      }
    }))
  }

  async function getConnectionStatus(id: string): Promise<ConnectionStatus> {
    try {
      loading.value = true
      error.value = null
      return await connectionService.getConnectionStatus(id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to get connection status'
      console.error('연결 상태 조회 실패:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createTestConnection() {
    try {
      loading.value = true
      error.value = null
      const testRequest: CreateConnectionRequest = {
        name: 'Local Kafka',
        host: 'localhost',
        port: 9092,
        sslEnabled: false,
        saslEnabled: false
      }
      const newConnection = await connectionService.createConnection(testRequest)
      connections.value.push(newConnection)
      return newConnection
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to create test connection'
      console.error('테스트 연결 생성 실패:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function setCurrentConnection(connection: ConnectionDto | null) {
    currentConnection.value = connection
  }

  function clearError() {
    error.value = null
  }

  return {
    connections,
    currentConnection,
    loading,
    error,
    fetchConnections,
    createConnection,
    updateConnection,
    deleteConnection,
    testConnection,
    getConnectionStatus,
    judgeConnections,
    createTestConnection,
    setCurrentConnection,
    clearError
  }
})