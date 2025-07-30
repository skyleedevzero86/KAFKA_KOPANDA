export const API_BASE_URL = '/api'

export const WEBSOCKET_URL = '/ws'

export const DEFAULT_PAGE_SIZE = 20

export const MAX_MESSAGE_LENGTH = 1000

export const REFRESH_INTERVAL = 30000 // 30초

export const CHART_COLORS = {
    primary: '#409EFF',
    success: '#67C23A',
    warning: '#E6A23C',
    danger: '#F56C6C',
    info: '#909399'
}

export const CONNECTION_STATUS = {
    CONNECTED: 'CONNECTED',
    DISCONNECTED: 'DISCONNECTED',
    ERROR: 'ERROR',
    CONNECTING: 'CONNECTING'
} as const

export const OFFSET_TYPES = {
    EARLIEST: 'EARLIEST',
    LATEST: 'LATEST',
    SPECIFIC: 'SPECIFIC'
} as const