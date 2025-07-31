import { apiService } from './api'
import type { ConnectionDto, CreateConnectionRequest, UpdateConnectionRequest, ConnectionTestResult, ConnectionStatus } from '@/types/connection'

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
    return apiService.get<ConnectionStatus>(`/connections/${id}/status`) 
  }

  async createTestConnection(): Promise<ConnectionDto> {
    const testRequest: CreateConnectionRequest = {
      name: 'Local Kafka',
      host: 'localhost',
      port: 9092,
      sslEnabled: false,
      saslEnabled: false
    }
    return this.createConnection(testRequest)
  }
}

export const connectionService = new ConnectionService()