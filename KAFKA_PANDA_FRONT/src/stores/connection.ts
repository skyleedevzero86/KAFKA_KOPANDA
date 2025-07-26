import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { connectionService } from '@/services/connectionService'
import type {
  ConnectionDto,
  CreateConnectionRequest,
  UpdateConnectionRequest,
  ConnectionStatus,
  ConnectionTestResult
} from '@/types/connection'

export const useConnectionStore = defineStore('connection', () => {
  const connections = ref<ConnectionDto[]>([])
  const currentConnection = ref<ConnectionDto | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const connectedConnections = computed(() => 
    connections.value.filter(conn => conn.lastConnected)
  )

  const disconnectedConnections = computed(() => 
    connections.value.filter(conn => !conn.lastConnected)
  )

  async function fetchConnections() {
    loading.value = true
    error.value = null
    try {
      connections.value = await connectionService.getConnections()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '연결 목록을 불러오는데 실패했습니다.'
    } finally {
      loading.value = false
    }
  }

  async function createConnection(request: CreateConnectionRequest) {
    loading.value = true
    error.value = null
    try {
      const newConnection = await connectionService.createConnection(request)
      connections.value.push(newConnection)
      return newConnection
    } catch (err) {
      error.value = err instanceof Error ? err.message : '연결 생성에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateConnection(id: string, request: UpdateConnectionRequest) {
    loading.value = true
    error.value = null
    try {
      const updatedConnection = await connectionService.updateConnection(id, request)
      const index = connections.value.findIndex(conn => conn.id === id)
      if (index !== -1) {
        connections.value[index] = updatedConnection
      }
      return updatedConnection
    } catch (err) {
      error.value = err instanceof Error ? err.message : '연결 수정에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteConnection(id: string) {
    loading.value = true
    error.value = null
    try {
      await connectionService.deleteConnection(id)
      connections.value = connections.value.filter(conn => conn.id !== id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '연결 삭제에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function testConnection(request: CreateConnectionRequest): Promise<ConnectionTestResult> {
    loading.value = true
    error.value = null
    try {
      return await connectionService.testConnection(request)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '연결 테스트에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getConnectionStatus(id: string): Promise<ConnectionStatus> {
    try {
      return await connectionService.getConnectionStatus(id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '연결 상태 조회에 실패했습니다.'
      throw err
    }
  }

  async function createTestConnection() {
    loading.value = true
    error.value = null
    try {
      const testConnection = await connectionService.createTestConnection()
      connections.value.push(testConnection)
      return testConnection
    } catch (err) {
      error.value = err instanceof Error ? err.message : '테스트 연결 생성에 실패했습니다.'
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
    connectedConnections,
    disconnectedConnections,
    fetchConnections,
    createConnection,
    updateConnection,
    deleteConnection,
    testConnection,
    getConnectionStatus,
    createTestConnection,
    setCurrentConnection,
    clearError
  }
})