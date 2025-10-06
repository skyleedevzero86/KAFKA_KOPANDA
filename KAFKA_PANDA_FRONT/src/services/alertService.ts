import { apiService } from './api'
import type { 
  AlertDto, 
  AcknowledgeAlertRequest, 
  ClearAllAlertsRequest 
} from '@/types/alert'

export class AlertService {
  async getActiveAlerts(connectionId?: string): Promise<AlertDto[]> {
    const params = connectionId ? { connectionId } : {}
    return apiService.get<AlertDto[]>('/api/alerts', params)
  }

  async getAlertsBySeverity(severity: string): Promise<AlertDto[]> {
    return apiService.get<AlertDto[]>(`/api/alerts/severity/${severity}`)
  }

  async getAlertById(id: string): Promise<AlertDto> {
    return apiService.get<AlertDto>(`/api/alerts/${id}`)
  }

  async acknowledgeAlert(request: AcknowledgeAlertRequest): Promise<AlertDto> {
    return apiService.post<AlertDto>(`/api/alerts/${request.alertId}/acknowledge?acknowledgedBy=${request.acknowledgedBy}`)
  }

  async clearAllAlerts(request: ClearAllAlertsRequest = {}): Promise<void> {
    const params = request.connectionId ? { connectionId: request.connectionId } : {}
    return apiService.delete<void>('/api/alerts', params)
  }

  async checkAndCreateAlerts(connectionId: string): Promise<void> {
    return apiService.post<void>(`/api/alerts/check/${connectionId}`)
  }

  async createTestAlert(connectionId: string): Promise<AlertDto> {
    return apiService.post<AlertDto>(`/api/alerts/test?connectionId=${connectionId}`) 
  }
}

export const alertService = new AlertService()
