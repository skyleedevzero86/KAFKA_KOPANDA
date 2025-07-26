export const KAFKA_CONSTANTS = {
  DEFAULT_PORT: 9092,
  DEFAULT_SSL_PORT: 9093,
  MAX_PARTITIONS: 1000,
  MAX_REPLICATION_FACTOR: 10,
  DEFAULT_MESSAGE_LIMIT: 100,
  MAX_MESSAGE_LIMIT: 1000
}

export const UI_CONSTANTS = {
  REFRESH_INTERVAL: 30000,
  CONNECTION_TIMEOUT: 10000,
  MAX_RETRY_ATTEMPTS: 3,
  DEBOUNCE_DELAY: 300
}

export const MESSAGE_TYPES = {
  TEXT: 'text',
  JSON: 'json',
  BINARY: 'binary'
}

export const OFFSET_TYPES = {
  EARLIEST: 'earliest',
  LATEST: 'latest',
  SPECIFIC: 'specific'
}

export const CONNECTION_STATUS = {
  CONNECTED: 'connected',
  DISCONNECTED: 'disconnected',
  CONNECTING: 'connecting',
  ERROR: 'error'
}

export const TOPIC_STATUS = {
  HEALTHY: 'healthy',
  UNHEALTHY: 'unhealthy',
  UNDER_REPLICATED: 'under_replicated'
}
