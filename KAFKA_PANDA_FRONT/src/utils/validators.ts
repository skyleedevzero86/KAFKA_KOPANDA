export function isValidHost(host: string): boolean {
    const hostRegex = /^[a-zA-Z0-9.-]+$/
    return hostRegex.test(host)
}

export function isValidPort(port: number): boolean {
    return port >= 1 && port <= 65535
}

export function isValidTopicName(name: string): boolean {
    const topicRegex = /^[a-zA-Z0-9._-]+$/
    return topicRegex.test(name) && name.length <= 249 && !name.startsWith('.') && !name.startsWith('_')
}

export function isValidConnectionName(name: string): boolean {
    const nameRegex = /^[a-zA-Z0-9가-힣\s_-]+$/
    return nameRegex.test(name) && name.length <= 100
}

export function isValidMessageKey(key: string): boolean {
    return key.length <= 1024
}

export function isValidMessageValue(value: string): boolean {
    return value.length <= 1048576 // 1MB
}

export function validateConnectionRequest(request: any): string[] {
    const errors: string[] = []

    if (!request.name || !isValidConnectionName(request.name)) {
        errors.push('유효하지 않은 연결 이름입니다.')
    }

    if (!request.host || !isValidHost(request.host)) {
        errors.push('유효하지 않은 호스트입니다.')
    }

    if (!request.port || !isValidPort(request.port)) {
        errors.push('유효하지 않은 포트입니다.')
    }

    if (request.saslEnabled && (!request.username || !request.password)) {
        errors.push('SASL이 활성화된 경우 사용자명과 비밀번호가 필요합니다.')
    }

    return errors
}

export function validateTopicRequest(request: any): string[] {
    const errors: string[] = []

    if (!request.name || !isValidTopicName(request.name)) {
        errors.push('유효하지 않은 토픽 이름입니다.')
    }

    if (!request.partitions || request.partitions < 1 || request.partitions > 100) {
        errors.push('파티션 수는 1-100 사이여야 합니다.')
    }

    if (!request.replicationFactor || request.replicationFactor < 1 || request.replicationFactor > 10) {
        errors.push('복제 팩터는 1-10 사이여야 합니다.')
    }

    return errors
}