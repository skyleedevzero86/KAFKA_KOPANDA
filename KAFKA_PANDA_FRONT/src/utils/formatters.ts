import dayjs from 'dayjs'

export function formatDate(date: string | Date): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

export function formatRelativeTime(date: string | Date): string {
  if (!date) return '-'
  return dayjs(date).fromNow()
}

export function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 Bytes'
  
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export function formatNumber(num: number): string {
  return num.toLocaleString()
}

export function formatMessageKey(key: string | null | undefined): string {
  if (!key) return '-'
  if (key.length > 50) {
    return key.substring(0, 50) + '...'
  }
  return key
}

export function formatMessageValue(value: string): string {
  if (!value) return '-'
  
  try {
    const parsed = JSON.parse(value)
    return JSON.stringify(parsed, null, 2)
  } catch {
    if (value.length > 200) {
      return value.substring(0, 200) + '...'
    }
    return value
  }
}

export function formatOffset(offset: number): string {
  return offset.toLocaleString()
}

export function formatPartition(partition: number): string {
  return `Partition ${partition}`
}

export function formatConnectionName(name: string): string {
  if (name.length > 30) {
    return name.substring(0, 30) + '...'
  }
  return name
}

export function formatTopicName(name: string): string {
  if (name.length > 40) {
    return name.substring(0, 40) + '...'
  }
  return name
}