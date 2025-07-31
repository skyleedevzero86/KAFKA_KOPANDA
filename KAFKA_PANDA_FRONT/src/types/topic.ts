export interface TopicDto {
  name: string
  partitionCount: number
  replicationFactor: number
  messageCount: number
  isInternal: boolean
  isHealthy: boolean
  createdAt: string
  updatedAt: string
}

export interface TopicDetailDto {
  name: string
  partitionCount: number
  replicationFactor: number
  messageCount: number
  isInternal: boolean
  isHealthy: boolean
  config: Record<string, string>
  partitions: PartitionDto[]
  createdAt: string
  updatedAt: string
}

export interface PartitionDto {
  partitionNumber: number
  leader: number
  replicas: number[]
  inSyncReplicas: number[]
  earliestOffset: number
  latestOffset: number
  messageCount: number
  isHealthy: boolean
  isUnderReplicated: boolean
}

export interface CreateTopicRequest {
  name: string
  partitions: number
  replicationFactor: number
  config: Record<string, string>
}

export interface DeleteTopicRequest {
  name: string
}