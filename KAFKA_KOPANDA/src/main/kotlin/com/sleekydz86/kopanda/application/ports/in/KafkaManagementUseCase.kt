package com.sleekydz86.kopanda.application.ports.`in`

import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.application.dto.common.OffsetType
import com.sleekydz86.kopanda.application.dto.request.CreateTopicRequest
import com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto
import com.sleekydz86.kopanda.application.dto.request.SendMessageRequest
import com.sleekydz86.kopanda.application.dto.response.*

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
    suspend fun createTestConsumerGroup(connectionId: String, topicName: String): ConsumerGroupDto
    suspend fun getMetrics(connectionId: String): KafkaMetricsDto

    suspend fun getTopicMetrics(connectionId: String): TopicMetricsDto

    suspend fun getDetailedMetrics(connectionId: String): DetailedMetricsDto
    suspend fun getTopicHealth(connectionId: String, topicName: String): TopicHealthDto
    suspend fun getAllTopicsHealth(connectionId: String): List<TopicHealthDto>
    suspend fun getConsumerGroupMetrics(connectionId: String, groupId: String): ConsumerGroupMetricsDto
    suspend fun getAllConsumerGroupMetrics(connectionId: String): List<ConsumerGroupMetricsDto>
    suspend fun getPerformanceMetrics(connectionId: String): PerformanceMetricsDto
    suspend fun getPartitionDetails(connectionId: String, topicName: String, partitionNumber: Int): PartitionDetailDto
    suspend fun getOffsetInfo(connectionId: String, topicName: String, partitionNumber: Int): OffsetInfoDto
    suspend fun setOffset(connectionId: String, topicName: String, partitionNumber: Int, offset: Long): Boolean
    suspend fun getClusterInfo(connectionId: String): ClusterInfoDto
}