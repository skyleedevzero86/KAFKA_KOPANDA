export interface ConnectionDto {
  id: string
  name: string
  host: string
  port: number
  sslEnabled: boolean
  saslEnabled: boolean
  username?: string
  createdAt: string
  updatedAt: string
  lastConnected?: string
}

export interface CreateConnectionRequest {
  name: string
  host: string
  port: number
  sslEnabled: boolean
  saslEnabled: boolean
  username?: string
  password?: string
}

export interface UpdateConnectionRequest {
  name?: string
  host?: string
  port?: number
  sslEnabled?: boolean
  saslEnabled?: boolean
  username?: string
  password?: string
}

export interface ConnectionStatus {
  connectionId: string
  status: ConnectionStatusType
  lastChecked: string
  errorMessage?: string
  brokerCount?: number
  topicCount?: number
  latency?: number
}

export enum ConnectionStatusType {
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
  CONNECTING = 'CONNECTING'
}

export interface ConnectionTestResult {
  success: boolean
  message: string
  latency?: number
  brokerInfo?: BrokerInfo
}

export interface BrokerInfo {
  count: number
  versions: string[]
}