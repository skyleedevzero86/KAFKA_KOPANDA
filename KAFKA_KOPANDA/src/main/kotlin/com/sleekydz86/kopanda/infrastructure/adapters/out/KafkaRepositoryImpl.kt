package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupDto
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto
import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.application.ports.out.KafkaRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Message
import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import com.sleekydz86.kopanda.domain.valueobjects.ids.BrokerId
import com.sleekydz86.kopanda.domain.valueobjects.topic.TopicConfig
import com.sleekydz86.kopanda.domain.valueobjects.message.MessageKey
import com.sleekydz86.kopanda.domain.valueobjects.message.MessageValue
import com.sleekydz86.kopanda.domain.valueobjects.message.Timestamp
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Repository
class KafkaRepositoryImpl : KafkaRepository {

    override suspend fun getTopics(connection: Connection): List<Topic> {
        val adminClient = createAdminClient(connection)
        return try {
            val topicList = adminClient.listTopics().names().get()
            topicList.map { topicName ->
                val topicDetails = adminClient.describeTopics(listOf(topicName)).all().get()[topicName]
                val partitions = topicDetails?.partitions()?.map { partitionInfo ->
                    com.sleekydz86.kopanda.domain.entities.Partition(
                        partitionNumber = PartitionNumber(partitionInfo.partition()),
                        leader = BrokerId(partitionInfo.leader().id()),
                        replicas = partitionInfo.replicas().map { BrokerId(it.id()) },
                        inSyncReplicas = partitionInfo.isr().map { BrokerId(it.id()) },
                        earliestOffset = 0,
                        latestOffset = 0
                    )
                } ?: emptyList()

                Topic(
                    name = TopicName(topicName),
                    config = TopicConfig(
                        partitionCount = partitions.size,
                        replicationFactor = partitions.firstOrNull()?.replicas?.size ?: 1,
                        config = emptyMap()
                    )
                ).apply {
                    partitions.forEach { addPartition(it) }
                }
            }
        } finally {
            adminClient.close()
        }
    }


override suspend fun getTopicDetails(connection: Connection, topicName: TopicName): Topic? {
    val adminClient = createAdminClient(connection)
    val consumer = createConsumer(connection)
    
    return try {
        val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]
        if (topicDetails == null) return null

        val partitions = topicDetails.partitions().map { partitionInfo ->
            val partitionNumber = partitionInfo.partition()
            val topicPartition = TopicPartition(topicName.value, partitionNumber)
            
            consumer.assign(listOf(topicPartition))
            val beginningOffsets = consumer.beginningOffsets(listOf(topicPartition))
            val endOffsets = consumer.endOffsets(listOf(topicPartition))
            
            val earliestOffset = beginningOffsets[topicPartition] ?: 0L
            val latestOffset = endOffsets[topicPartition] ?: 0L
            val messageCount = latestOffset - earliestOffset

            com.sleekydz86.kopanda.domain.entities.Partition(
                partitionNumber = PartitionNumber(partitionNumber),
                leader = BrokerId(partitionInfo.leader().id()),
                replicas = partitionInfo.replicas().map { BrokerId(it.id()) },
                inSyncReplicas = partitionInfo.isr().map { BrokerId(it.id()) },
                earliestOffset = earliestOffset,
                latestOffset = latestOffset,
                messageCount = messageCount
            )
        }

        Topic(
            name = topicName,
            config = TopicConfig(
                partitionCount = partitions.size,
                replicationFactor = partitions.firstOrNull()?.replicas?.size ?: 1,
                config = emptyMap()
            )
        ).apply {
            partitions.forEach { addPartition(it) }
        }
    } finally {
        adminClient.close()
        consumer.close()
    }
}


    override suspend fun createTopic(connection: Connection, topic: Topic): Topic {
        val adminClient = createAdminClient(connection)
        return try {
            val newTopic = NewTopic(
                topic.name.value,
                topic.config.partitionCount,
                topic.config.replicationFactor.toShort()
            ).configs(topic.config.config)

            adminClient.createTopics(listOf(newTopic)).all().get()
            topic
        } finally {
            adminClient.close()
        }
    }

    override suspend fun deleteTopic(connection: Connection, topicName: TopicName) {
        val adminClient = createAdminClient(connection)
        try {
            adminClient.deleteTopics(listOf(topicName.value)).all().get()
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getMessages(
        connection: Connection,
        topic: Topic,
        partition: com.sleekydz86.kopanda.domain.entities.Partition,
        offset: Offset,
        limit: Int
    ): List<Message> {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(topic.name.value, partition.partitionNumber.value)
            consumer.assign(listOf(topicPartition))
            consumer.seek(topicPartition, offset.value)

            val records = consumer.poll(Duration.ofSeconds(5))
            records.records(topicPartition).take(limit).map { record ->
                Message.create(
                    offset = record.offset(),
                    key = record.key(),
                    value = record.value() ?: "",
                    timestamp = record.timestamp(),
                    partitionNumber = record.partition(),
                    headers = emptyMap()
                )
            }
        } finally {
            consumer.close()
        }
    }

    override suspend fun sendMessage(
        connection: Connection,
        topic: Topic,
        key: String?,
        value: String,
        partition: PartitionNumber?,
        headers: Map<String, String>
    ) {
        val producer = createProducer(connection)
        try {
            val record = ProducerRecord(
                topic.name.value,
                partition?.value,
                key,
                value
            )
            headers.forEach { (k, v) -> record.headers().add(k, v.toByteArray()) }
            producer.send(record).get()
        } finally {
            producer.close()
        }
    }

    override suspend fun searchMessages(connection: Connection, criteria: MessageSearchCriteria): List<Message> {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(criteria.topic, criteria.partition ?: 0)
            consumer.assign(listOf(topicPartition))

            if (criteria.startOffset != null) {
                consumer.seek(topicPartition, criteria.startOffset)
            } else {
                consumer.seekToBeginning(listOf(topicPartition))
            }

            val messages = mutableListOf<Message>()
            val records = consumer.poll(Duration.ofSeconds(5))

            records.records(topicPartition).take(criteria.limit).forEach { record ->
                val message = Message.create(
                    offset = record.offset(),
                    key = record.key(),
                    value = record.value() ?: "",
                    timestamp = record.timestamp(),
                    partitionNumber = record.partition(),
                    headers = emptyMap()
                )

                if (criteria.key == null || record.key()?.contains(criteria.key) == true) {
                    if (criteria.value == null || record.value()?.contains(criteria.value) == true) {
                        messages.add(message)
                    }
                }
            }

            messages
        } finally {
            consumer.close()
        }
    }

    override suspend fun getConsumerGroups(connection: Connection): List<ConsumerGroupDto> {
        val adminClient = createAdminClient(connection)
        return try {
            val consumerGroups = adminClient.listConsumerGroups().all().get()

            val groupIds = consumerGroups.map { it.groupId() }
            val groupDescriptions = if (groupIds.isNotEmpty()) {
                adminClient.describeConsumerGroups(groupIds).all().get()
            } else {
                emptyMap()
            }

            consumerGroups.map { group ->
                val description = groupDescriptions[group.groupId()]
                ConsumerGroupDto(
                    groupId = group.groupId(),
                    state = description?.state()?.toString() ?: group.state()?.toString() ?: "UNKNOWN",
                    memberCount = description?.members()?.size ?: 0,
                    topicCount = description?.members()
                        ?.flatMap { it.assignment().topicPartitions() }
                        ?.map { it.topic() }
                        ?.distinct()
                        ?.size ?: 0,
                    offsets = emptyMap()
                )
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getMetrics(connection: Connection): KafkaMetricsDto {
        return KafkaMetricsDto(
            brokerCount = 1,
            topicCount = 0,
                totalPartitions = 0,
                messagesPerSecond = 0.0,
                bytesInPerSec = 0.0,
                bytesOutPerSec = 0.0,
                activeConnections = 0,
                timestamp = LocalDateTime.now()
            )
    }

    override suspend fun testConnection(connection: Connection): Boolean {
        return try {
            val adminClient = createAdminClient(connection)
            adminClient.listTopics().names().get()
            adminClient.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getDetailedMetrics(connection: Connection): com.sleekydz86.kopanda.application.dto.response.DetailedMetricsDto {
        return com.sleekydz86.kopanda.application.dto.response.DetailedMetricsDto(
            connectionId = connection.getId().value,
            brokerMetrics = com.sleekydz86.kopanda.application.dto.response.BrokerMetricsDto(
                totalBrokers = 1,
                activeBrokers = 1,
                offlineBrokers = 0,
                averageResponseTime = 0L,
                totalDiskUsage = 0L,
                availableDiskSpace = 0L
            ),
            topicMetrics = com.sleekydz86.kopanda.application.dto.response.TopicMetricsDto(
                totalTopics = 0,
                internalTopics = 0,
                userTopics = 0,
                totalPartitions = 0,
                underReplicatedPartitions = 0,
                offlinePartitions = 0,
                totalMessages = 0L,
                messagesPerSecond = 0.0
            ),
            partitionMetrics = com.sleekydz86.kopanda.application.dto.response.PartitionMetricsDto(
                totalPartitions = 0,
                healthyPartitions = 0,
                underReplicatedPartitions = 0,
                offlinePartitions = 0,
                averageReplicationFactor = 0.0,
                totalMessages = 0L,
                averageMessagesPerPartition = 0.0
            ),
            performanceMetrics = com.sleekydz86.kopanda.application.dto.response.PerformanceMetricsDto(
                messagesPerSecond = 0.0,
                bytesInPerSec = 0.0,
                bytesOutPerSec = 0.0,
                requestsPerSec = 0.0,
                averageRequestLatency = 0L,
                maxRequestLatency = 0L,
                activeConnections = 0,
                totalConnections = 0
            ),
            timestamp = LocalDateTime.now()
        )
    }

    override suspend fun getTopicHealth(connection: Connection, topicName: TopicName): com.sleekydz86.kopanda.application.dto.response.TopicHealthDto {
        return com.sleekydz86.kopanda.application.dto.response.TopicHealthDto(
            topicName = topicName.value,
            isHealthy = true,
            healthScore = 100,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            totalPartitions = 0,
            replicationFactor = 1,
            averageReplicationFactor = 1.0,
            lastUpdated = LocalDateTime.now(),
            issues = emptyList()
        )
    }

    override suspend fun getAllTopicsHealth(connection: Connection): List<com.sleekydz86.kopanda.application.dto.response.TopicHealthDto> {
        return emptyList()
    }

    override suspend fun getConsumerGroupMetrics(connection: Connection, groupId: String): com.sleekydz86.kopanda.application.dto.response.ConsumerGroupMetricsDto {
        return com.sleekydz86.kopanda.application.dto.response.ConsumerGroupMetricsDto(
            groupId = groupId,
            state = "Empty",
            memberCount = 0,
            topicCount = 0,
            totalLag = 0L,
            averageLag = 0.0,
            maxLag = 0L,
            minLag = 0L,
            lastCommitTime = null,
            partitions = emptyList(),
            members = emptyList()
        )
    }

    override suspend fun getAllConsumerGroupMetrics(connection: Connection): List<com.sleekydz86.kopanda.application.dto.response.ConsumerGroupMetricsDto> {
        return emptyList()
    }

    override suspend fun getPerformanceMetrics(connection: Connection): com.sleekydz86.kopanda.application.dto.response.PerformanceMetricsDto {
        return com.sleekydz86.kopanda.application.dto.response.PerformanceMetricsDto(
            messagesPerSecond = 0.0,
            bytesInPerSec = 0.0,
            bytesOutPerSec = 0.0,
            requestsPerSec = 0.0,
            averageRequestLatency = 0L,
            maxRequestLatency = 0L,
            activeConnections = 0,
            totalConnections = 0
        )
    }

    override suspend fun getPartitionDetails(connection: Connection, topicName: TopicName, partitionNumber: Int): com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto {
        return com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto(
            topicName = topicName.value,
            partitionNumber = partitionNumber,
            leader = 0,
            replicas = emptyList(),
            inSyncReplicas = emptyList(),
            earliestOffset = 0L,
            latestOffset = 0L,
            messageCount = 0L,
            isHealthy = true,
            isUnderReplicated = false,
            lastUpdated = LocalDateTime.now()
        )
    }

    override suspend fun getOffsetInfo(connection: Connection, topicName: TopicName, partitionNumber: Int): com.sleekydz86.kopanda.application.dto.response.OffsetInfoDto {
        return com.sleekydz86.kopanda.application.dto.response.OffsetInfoDto(
            topicName = topicName.value,
            partitionNumber = partitionNumber,
            currentOffset = 0L,
            committedOffset = 0L,
            endOffset = 0L,
            lag = 0L,
            consumerGroup = null,
            lastCommitTime = null
        )
    }

    override suspend fun setOffset(connection: Connection, topicName: TopicName, partitionNumber: Int, offset: Long): Boolean {
        return true
    }

    override suspend fun getClusterInfo(connection: Connection): com.sleekydz86.kopanda.application.dto.response.ClusterInfoDto {
        return com.sleekydz86.kopanda.application.dto.response.ClusterInfoDto(
            clusterId = "test-cluster",
            controllerId = 0,
            totalBrokers = 1,
            activeBrokers = 1,
            totalTopics = 0,
            totalPartitions = 0,
            version = "2.8.0",
            lastUpdated = LocalDateTime.now()
        )
    }

    override suspend fun getBrokerMetrics(connection: Connection): com.sleekydz86.kopanda.application.dto.response.BrokerMetricsDto {
        return com.sleekydz86.kopanda.application.dto.response.BrokerMetricsDto(
            totalBrokers = 1,
            activeBrokers = 1,
            offlineBrokers = 0,
            averageResponseTime = 0L,
            totalDiskUsage = 0L,
            availableDiskSpace = 0L
        )
    }

    override suspend fun getTopicMetrics(connection: Connection): com.sleekydz86.kopanda.application.dto.response.TopicMetricsDto {
        return com.sleekydz86.kopanda.application.dto.response.TopicMetricsDto(
            totalTopics = 0,
            internalTopics = 0,
            userTopics = 0,
            totalPartitions = 0,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            totalMessages = 0L,
            messagesPerSecond = 0.0
        )
    }

    override suspend fun getPartitionMetrics(connection: Connection): com.sleekydz86.kopanda.application.dto.response.PartitionMetricsDto {
        return com.sleekydz86.kopanda.application.dto.response.PartitionMetricsDto(
            totalPartitions = 0,
            healthyPartitions = 0,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            averageReplicationFactor = 0.0,
            totalMessages = 0L,
            averageMessagesPerPartition = 0.0
        )
    }

    override suspend fun createTestConsumerGroup(connection: Connection, topicName: String): ConsumerGroupDto {
        return ConsumerGroupDto(
            groupId = "test-group",
            state = "Empty",
            memberCount = 0,
            topicCount = 0,
            offsets = emptyMap()
        )
    }

    private fun createAdminClient(connection: Connection): AdminClient {
    val props = Properties().apply {
        put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "${connection.host.value}:${connection.port.value}")
        put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000")
        put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000")
        put(AdminClientConfig.RETRIES_CONFIG, "3")
    }
    return AdminClient.create(props)
}

    private fun createConsumer(connection: Connection): KafkaConsumer<String, String> {
        val config = Properties()
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "${connection.host.value}:${connection.port.value}"
        config[ConsumerConfig.GROUP_ID_CONFIG] = "kopanda-consumer"
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return KafkaConsumer(config)
    }

    private fun createProducer(connection: Connection): KafkaProducer<String, String> {
        val config = Properties()
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "${connection.host.value}:${connection.port.value}"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        return KafkaProducer(config)
    }
}