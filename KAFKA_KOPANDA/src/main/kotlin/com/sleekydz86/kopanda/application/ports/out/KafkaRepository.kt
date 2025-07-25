package com.sleekydz86.kopanda.application.ports.out


import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.domain.entities.Message
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupDto
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto

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
}