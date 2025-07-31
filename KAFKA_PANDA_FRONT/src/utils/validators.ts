
export function validateTopicName(name: string): boolean {
  if (!name || name.length < 1 || name.length > 50) {
    return false
  }
  
  const topicNameRegex = /^[a-zA-Z0-9._-]+$/
  return topicNameRegex.test(name)
}

export function validateHost(host: string): boolean {
  if (!host) return false
  
  const ipRegex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
  const domainRegex = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/
  
  return ipRegex.test(host) || domainRegex.test(host) || host === 'localhost'
}

export function validatePort(port: number): boolean {
  return port >= 1 && port <= 65535
}

export function validateConnectionName(name: string): boolean {
  return Boolean(name && name.length >= 1 && name.length <= 50)
}

export function validateMessageKey(key: string): boolean {
  return key === null || key === undefined || (typeof key === 'string' && key.length <= 1000)
}

export function validateMessageValue(value: string): boolean {
  return Boolean(value && value.length > 0 && value.length <= 1000000) 
}

export function validatePartitionNumber(partition: number, maxPartitions: number): boolean {
  return partition >= 0 && partition < maxPartitions
}

export function validateOffset(offset: number): boolean {
  return offset >= 0
}