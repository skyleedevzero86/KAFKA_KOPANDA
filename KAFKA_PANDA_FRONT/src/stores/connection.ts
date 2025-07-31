import { defineStore } from 'pinia'
import { ref } from 'vue'
import { connectionService } from '@/services/connectionService'
import type { ConnectionDto, CreateConnectionRequest, UpdateConnectionRequest, ConnectionTestResult, ConnectionStatus } from '@/types/connection'

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
      console.error('Failed to fetch connections:', err)
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
      console.error('Failed to create connection:', err)
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
      console.error('Failed to update connection:', err)
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
      console.error('Failed to delete connection:', err)
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
      console.error('Failed to test connection:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getConnectionStatus(id: string): Promise<ConnectionStatus> {
    try {
      loading.value = true
      error.value = null
      return await connectionService.getConnectionStatus(id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to get connection status'
      console.error('Failed to get connection status:', err)
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
      console.error('Failed to create test connection:', err)
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
    createTestConnection,
    setCurrentConnection,
    clearError
  }
})