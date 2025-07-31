export interface KafkaMetricsDto {
  brokerCount: number
  topicCount: number
  totalPartitions: number
  messagesPerSecond: number
  bytesInPerSec: number
  bytesOutPerSec: number
  activeConnections: number
  timestamp: string
}

export interface ConsumerGroupDto {
  groupId: string
  state: string
  memberCount: number
  topicCount: number
  offsets: Record<string, number>
}

export interface ConsumerGroupDetailDto {
  groupId: string
  state: string
  members: ConsumerMemberDto[]
  offsets: Record<string, number>
}

export interface ConsumerMemberDto {
  memberId: string
  clientId: string
  clientHost: string
  partitions: string[]
}