export interface AlertDto {
  id: string
  type: IssueType
  severity: IssueSeverity
  title: string
  message: string
  connectionId?: string
  topicName?: string
  partitionNumber?: number
  timestamp: string
  isAcknowledged: boolean
  acknowledgedAt?: string
  acknowledgedBy?: string
}

export enum IssueType {
  UNDER_REPLICATED = 'UNDER_REPLICATED',
  OFFLINE_PARTITION = 'OFFLINE_PARTITION',
  LEADER_NOT_AVAILABLE = 'LEADER_NOT_AVAILABLE',
  REPLICATION_FACTOR_MISMATCH = 'REPLICATION_FACTOR_MISMATCH',
  CONSUMER_LAG = 'CONSUMER_LAG',
  CONNECTION_ERROR = 'CONNECTION_ERROR'
}

export enum IssueSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export interface CreateTestAlertRequest {
  connectionId: string
}

export interface AcknowledgeAlertRequest {
  alertId: string
  acknowledgedBy: string
}

export interface ClearAllAlertsRequest {
  connectionId?: string
}
