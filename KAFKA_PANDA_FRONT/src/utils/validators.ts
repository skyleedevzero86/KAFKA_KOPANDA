export function validateConnectionConfig(config: {
  name: string
  host: string
  port: number
  sslEnabled: boolean
  saslEnabled: boolean
  username?: string
  password?: string
}): { isValid: boolean; errors: string[] } {
  const errors: string[] = []

  if (!config.name || config.name.trim().length === 0) {
    errors.push('연결 이름은 필수입니다.')
  } else if (config.name.length > 50) {
    errors.push('연결 이름은 50자를 초과할 수 없습니다.')
  }

  if (!config.host || config.host.trim().length === 0) {
    errors.push('호스트는 필수입니다.')
  } else if (!isValidHost(config.host)) {
    errors.push('유효한 호스트 주소를 입력해주세요.')
  }

  if (!config.port || config.port < 1 || config.port > 65535) {
    errors.push('포트는 1-65535 사이의 값이어야 합니다.')
  }

  if (config.saslEnabled) {
    if (!config.username || config.username.trim().length === 0) {
      errors.push('SASL이 활성화된 경우 사용자명은 필수입니다.')
    }
    if (!config.password || config.password.trim().length === 0) {
      errors.push('SASL이 활성화된 경우 비밀번호는 필수입니다.')
    }
  }

  return {
    isValid: errors.length === 0,
    errors
  }
}

export function validateTopicConfig(config: {
  name: string
  partitions: number
  replicationFactor: number
}): { isValid: boolean; errors: string[] } {
  const errors: string[] = []

  if (!config.name || config.name.trim().length === 0) {
    errors.push('토픽 이름은 필수입니다.')
  } else if (!isValidTopicName(config.name)) {
    errors.push('토픽 이름은 영문자, 숫자, 하이픈, 언더스코어만 사용할 수 있습니다.')
  } else if (config.name.length > 249) {
    errors.push('토픽 이름은 249자를 초과할 수 없습니다.')
  }

  if (!config.partitions || config.partitions < 1 || config.partitions > 1000) {
    errors.push('파티션 수는 1-1000 사이의 값이어야 합니다.')
  }

  if (!config.replicationFactor || config.replicationFactor < 1 || config.replicationFactor > 10) {
    errors.push('복제 팩터는 1-10 사이의 값이어야 합니다.')
  }

  return {
    isValid: errors.length === 0,
    errors
  }
}

export function validateMessageConfig(config: {
  topic: string
  value: string
  key?: string
  partition?: number
}): { isValid: boolean; errors: string[] } {
  const errors: string[] = []

  if (!config.topic || config.topic.trim().length === 0) {
    errors.push('토픽은 필수입니다.')
  }

  if (!config.value || config.value.trim().length === 0) {
    errors.push('메시지 값은 필수입니다.')
  }

  if (config.partition !== undefined && (config.partition < 0 || config.partition > 999)) {
    errors.push('파티션 번호는 0-999 사이의 값이어야 합니다.')
  }

  return {
    isValid: errors.length === 0,
    errors
  }
}

function isValidHost(host: string): boolean {
  const hostRegex = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$|^(\d{1,3}\.){3}\d{1,3}$/
  return hostRegex.test(host)
}

function isValidTopicName(name: string): boolean {
  const topicRegex = /^[a-zA-Z0-9._-]+$/
  return topicRegex.test(name)
}

export function isValidEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

export function isValidUrl(url: string): boolean {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}