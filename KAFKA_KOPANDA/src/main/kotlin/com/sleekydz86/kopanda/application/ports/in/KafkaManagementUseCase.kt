package com.sleekydz86.kopanda.application.ports.`in`

import com.sleekydz86.kopanda.application.dto.*

interface KafkaManagementUseCase {
    suspend fun getTopics(connectionId: String): List<TopicDto>

    suspend fun getTopicDetails(connectionId: String, topicName: String): TopicDetailDto

    suspend fun createTopic(connectionId: String, request: CreateTopicRequest): TopicDto

    suspend fun deleteTopic(connectionId: String, topicName: String)

    suspend fun getMessages(
        connectionId: String,
        topicName: String,
        partitionNumber: Int,
        offset: Long?,
        offsetType: OffsetType,
        limit: Int
    ): PaginatedResponse<MessageDto>

    suspend fun sendMessage(connectionId: String, topicName: String, request: SendMessageRequest)

    suspend fun searchMessages(connectionId: String, criteria: MessageSearchCriteria): List<MessageDto>

    suspend fun getConsumerGroups(connectionId: String): List<ConsumerGroupDto>

    suspend fun getMetrics(connectionId: String): KafkaMetricsDto
}