import dayjs from 'dayjs'

export function formatDate(date: string | Date): string {
    return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

export function formatNumber(num: number): string {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M'
    }
    if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K'
    }
    return num.toString()
}

export function formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B'

    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export function formatDuration(ms: number): string {
    if (ms < 1000) return ms + 'ms'
    if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
    if (ms < 3600000) return (ms / 60000).toFixed(1) + 'm'
    return (ms / 3600000).toFixed(1) + 'h'
}

export function formatPercentage(value: number, total: number): string {
    if (total === 0) return '0%'
    return ((value / total) * 100).toFixed(1) + '%'
}