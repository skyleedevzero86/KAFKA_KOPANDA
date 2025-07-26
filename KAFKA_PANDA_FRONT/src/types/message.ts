export interface MessageDto {
  offset: number
  key?: string
  value: string
  timestamp: number
  partition: number
  consumed: boolean
  headers: Record<string, string>
  formattedTime: string
  formattedOffset: string
}

export interface SendMessageRequest {
  key?: string
  value: string
  partition?: number
  headers: Record<string, string>
}

export interface MessageSearchCriteria {
  topic: string
  partition?: number
  key?: string
  value?: string
  startOffset?: number
  endOffset?: number
  startTime?: number
  endTime?: number
  limit: number
}

export enum OffsetType {
  EARLIEST = 'EARLIEST',
  LATEST = 'LATEST',
  SPECIFIC = 'SPECIFIC'
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}