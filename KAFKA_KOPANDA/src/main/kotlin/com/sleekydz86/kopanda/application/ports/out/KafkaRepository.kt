package com.sleekydz86.kopanda.application.ports.out

import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.domain.entities.Message

import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset

import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto
import com.sleekydz86.kopanda.application.dto.response.BrokerMetricsDto
import com.sleekydz86.kopanda.application.dto.response.ClusterInfoDto
import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupDto
import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupMetricsDto
import com.sleekydz86.kopanda.application.dto.response.DetailedMetricsDto
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto
import com.sleekydz86.kopanda.application.dto.response.OffsetInfoDto
import com.sleekydz86.kopanda.application.dto.response.PartitionMetricsDto
import com.sleekydz86.kopanda.application.dto.response.PerformanceMetricsDto
import com.sleekydz86.kopanda.application.dto.response.TopicHealthDto
import com.sleekydz86.kopanda.application.dto.response.TopicMetricsDto

interface KafkaRepository {
    suspend fun getTopics(connection: Connection): List<Topic>
    suspend fun getTopicDetails(connection: Connection, topicName: TopicName): Topic?
    suspend fun createTopic(connection: Connection, topic: Topic): Topic
    suspend fun deleteTopic(connection: Connection, topicName: TopicName)
    suspend fun getMessages(
        connection: Connection,
        topic: Topic,
        partition: com.sleekydz86.kopanda.domain.entities.Partition,
        offset: Offset,
        limit: Int
    ): List<Message>
    suspend fun sendMessage(
        connection: Connection,
        topic: Topic,
        key: String?,
        value: String,
        partition: PartitionNumber?,
        headers: Map<String, String>
    )
    suspend fun searchMessages(connection: Connection, criteria: MessageSearchCriteria): List<Message>
    suspend fun getConsumerGroups(connection: Connection): List<ConsumerGroupDto>
    suspend fun getMetrics(connection: Connection): KafkaMetricsDto
    suspend fun testConnection(connection: Connection): Boolean

    suspend fun getDetailedMetrics(connection: Connection): DetailedMetricsDto
    suspend fun getTopicHealth(connection: Connection, topicName: TopicName): TopicHealthDto
    suspend fun getAllTopicsHealth(connection: Connection): List<TopicHealthDto>
    suspend fun getConsumerGroupMetrics(connection: Connection, groupId: String): ConsumerGroupMetricsDto
    suspend fun getAllConsumerGroupMetrics(connection: Connection): List<ConsumerGroupMetricsDto>
    suspend fun getPerformanceMetrics(connection: Connection): PerformanceMetricsDto
    suspend fun getPartitionDetails(connection: Connection, topicName: TopicName, partitionNumber: Int): PartitionDetailDto
    suspend fun getOffsetInfo(connection: Connection, topicName: TopicName, partitionNumber: Int): OffsetInfoDto
    suspend fun setOffset(connection: Connection, topicName: TopicName, partitionNumber: Int, offset: Long): Boolean
    suspend fun getClusterInfo(connection: Connection): ClusterInfoDto
    suspend fun getBrokerMetrics(connection: Connection): BrokerMetricsDto
    suspend fun getTopicMetrics(connection: Connection): TopicMetricsDto
    suspend fun getPartitionMetrics(connection: Connection): PartitionMetricsDto
    suspend fun createTestConsumerGroup(connection: Connection, topicName: String): ConsumerGroupDto
}