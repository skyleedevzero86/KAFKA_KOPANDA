
export const API_BASE_URL = import.meta.env['VITE_API_BASE_URL'] || 'http://localhost:8080/api'

export const DEFAULT_PAGE_SIZE = 10
export const MAX_PAGE_SIZE = 100

export const DEFAULT_PARTITIONS = 3
export const DEFAULT_REPLICATION_FACTOR = 1
export const MAX_PARTITIONS = 100
export const MAX_REPLICATION_FACTOR = 10

export const DEFAULT_KAFKA_PORT = 9092
export const DEFAULT_ZOOKEEPER_PORT = 2181

export const DEFAULT_MESSAGE_LIMIT = 100
export const MAX_MESSAGE_LIMIT = 1000

export const CHART_COLORS = {
  primary: '#409EFF',
  success: '#67C23A',
  warning: '#E6A23C',
  danger: '#F56C6C',
  info: '#909399'
}