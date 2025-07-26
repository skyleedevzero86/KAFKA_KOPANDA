import { apiService } from './api'
import type {
  ConnectionDto,
  CreateConnectionRequest,
  UpdateConnectionRequest,
  ConnectionStatus,
  ConnectionTestResult
} from '@/types/connection'

export class ConnectionService {
  async getConnections(): Promise<ConnectionDto[]> {
    return apiService.get<ConnectionDto[]>('/connections')
  }

  async getConnection(id: string): Promise<ConnectionDto> {
    return apiService.get<ConnectionDto>(`/connections/${id}`)
  }

  async createConnection(request: CreateConnectionRequest): Promise<ConnectionDto> {
    return apiService.post<ConnectionDto>('/connections', request)
  }

  async updateConnection(id: string, request: UpdateConnectionRequest): Promise<ConnectionDto> {
    return apiService.put<ConnectionDto>(`/connections/${id}`, request)
  }

  async deleteConnection(id: string): Promise<void> {
    return apiService.delete<void>(`/connections/${id}`)
  }

  async testConnection(request: CreateConnectionRequest): Promise<ConnectionTestResult> {
    return apiService.post<ConnectionTestResult>('/connections/test', request)
  }

  async getConnectionStatus(id: string): Promise<ConnectionStatus> {
    return apiService.get<ConnectionStatus>(`/test/connection-status/${id}`)
  }

  async createTestConnection(): Promise<ConnectionDto> {
    return apiService.post<ConnectionDto>('/test/create-test-connection')
  }
}

export const connectionService = new ConnectionService()