package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.application.dto.response.*
import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.application.dto.enums.IssueSeverity
import com.sleekydz86.kopanda.application.dto.enums.IssueType
import com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto
import com.sleekydz86.kopanda.application.ports.out.KafkaRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Message
import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.ids.BrokerId
import com.sleekydz86.kopanda.domain.valueobjects.topic.TopicConfig
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import org.slf4j.LoggerFactory

@Repository
class KafkaRepositoryImpl(
    private val jmxMetricsCollector: JmxMetricsCollector
) : KafkaRepository {

    private val logger = LoggerFactory.getLogger(KafkaRepositoryImpl::class.java)

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
                        replicas = partitionInfo.replicas().map { broker -> BrokerId(broker.id()) },
                        inSyncReplicas = partitionInfo.isr().map { broker -> BrokerId(broker.id()) },
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
                    partitions.forEach { partition -> addPartition(partition) }
                }
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getTopicDetails(connection: Connection, topicName: TopicName): Topic? {
        val adminClient = createAdminClient(connection)
        return try {
            val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]
            if (topicDetails == null) return null

            val partitions = topicDetails.partitions().map { partitionInfo ->
                com.sleekydz86.kopanda.domain.entities.Partition(
                    partitionNumber = PartitionNumber(partitionInfo.partition()),
                    leader = BrokerId(partitionInfo.leader().id()),
                    replicas = partitionInfo.replicas().map { broker -> BrokerId(broker.id()) },
                    inSyncReplicas = partitionInfo.isr().map { broker -> BrokerId(broker.id()) },
                    earliestOffset = 0,
                    latestOffset = 0
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
                partitions.forEach { partition -> addPartition(partition) }
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun createTopic(connection: Connection, topic: Topic): Topic {
        val adminClient = createAdminClient(connection)
        return try {
            val newTopic = NewTopic(topic.name.value, topic.config.partitionCount, topic.config.replicationFactor.toShort())
            newTopic.configs(topic.config.config)
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
                    headers = record.headers().associate { header -> header.key() to String(header.value()) }
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
            val record = ProducerRecord(topic.name.value, partition?.value, key, value)
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
            var count = 0
            
            while (count < criteria.limit) {
                val records = consumer.poll(Duration.ofSeconds(1))
                val batch = records.records(topicPartition)
                
                for (record in batch) {
                    if (count >= criteria.limit) break
                    
                    val matchesKey = criteria.key == null || record.key()?.contains(criteria.key!!) == true
                    val matchesValue = criteria.value == null || record.value()?.contains(criteria.value!!) == true
                    
                    if (matchesKey && matchesValue) {
                        messages.add(Message.create(
                            offset = record.offset(),
                            key = record.key(),
                            value = record.value() ?: "",
                            timestamp = record.timestamp(),
                            partitionNumber = record.partition(),
                            headers = record.headers().associate { header -> header.key() to String(header.value()) }
                        ))
                        count++
                    }
                }
                
                if (records.isEmpty) break
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
            consumerGroups.map { groupListing ->
                val groupId = groupListing.groupId()
                val groupDescription = adminClient.describeConsumerGroups(listOf(groupId)).all().get()[groupId]
                ConsumerGroupDto(
                    groupId = groupId,
                    state = groupDescription?.state()?.name ?: "UNKNOWN",
                    memberCount = groupDescription?.members()?.size ?: 0,
                    topicCount = 0,
                    offsets = emptyMap()
                )
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getMetrics(connection: Connection): KafkaMetricsDto {
        val adminClient = createAdminClient(connection)
        return try {
            val clusterDescription = adminClient.describeCluster().nodes().get()
            val topicList = adminClient.listTopics().names().get()
            
            KafkaMetricsDto(
                brokerCount = clusterDescription.size,
                topicCount = topicList.size,
                totalPartitions = 0,
                messagesPerSecond = 0.0,
                bytesInPerSec = 0.0,
                bytesOutPerSec = 0.0,
                activeConnections = 0,
                timestamp = LocalDateTime.now()
            )
        } finally {
            adminClient.close()
        }
    }

    override suspend fun testConnection(connection: Connection): Boolean {
        val adminClient = createAdminClient(connection)
        return try {
            logger.info("Testing connection to ${connection.getConnectionString()}")
            adminClient.listTopics().names().get()
            logger.info("Connection test successful for ${connection.getConnectionString()}")
            true
        } catch (e: Exception) {
            logger.error("Connection test failed for ${connection.getConnectionString()}: ${e.message}", e)
            false
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getDetailedMetrics(connection: Connection): DetailedMetricsDto {
        val brokerMetrics = jmxMetricsCollector.collectBrokerMetrics(connection)
        val topicMetrics = jmxMetricsCollector.collectTopicMetrics(connection)
        val partitionMetrics = jmxMetricsCollector.collectPartitionMetrics(connection)
        val performanceMetrics = jmxMetricsCollector.collectPerformanceMetrics(connection)

        return DetailedMetricsDto(
            connectionId = connection.getId().value,
            brokerMetrics = brokerMetrics,
            topicMetrics = topicMetrics,
            partitionMetrics = partitionMetrics,
            performanceMetrics = performanceMetrics,
            timestamp = LocalDateTime.now()
        )
    }

    override suspend fun getBrokerMetrics(connection: Connection): BrokerMetricsDto {
        return jmxMetricsCollector.collectBrokerMetrics(connection)
    }

    override suspend fun getTopicMetrics(connection: Connection): TopicMetricsDto {
        return jmxMetricsCollector.collectTopicMetrics(connection)
    }

    override suspend fun getPartitionMetrics(connection: Connection): PartitionMetricsDto {
        return jmxMetricsCollector.collectPartitionMetrics(connection)
    }

    override suspend fun getPerformanceMetrics(connection: Connection): PerformanceMetricsDto {
        return PerformanceMetricsDto(
            messagesPerSecond = 0.0,
            bytesInPerSec = 0.0,
            bytesOutPerSec = 0.0,
            requestsPerSec = 0.0,
            averageRequestLatency = 0,
            maxRequestLatency = 0,
            activeConnections = 0,
            totalConnections = 0
        )
    }

    override suspend fun getTopicHealth(connection: Connection, topicName: TopicName): TopicHealthDto {
        val adminClient = createAdminClient(connection)
        return try {
            val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]

            if (topicDetails == null) {
                return TopicHealthDto(
                    topicName = topicName.value,
                    isHealthy = false,
                    healthScore = 0,
                    underReplicatedPartitions = 0,
                    offlinePartitions = 0,
                    totalPartitions = 0,
                    replicationFactor = 0,
                    averageReplicationFactor = 0.0,
                    lastUpdated = LocalDateTime.now(),
                    issues = listOf(
                        TopicIssueDto(
                            type = IssueType.LEADER_NOT_AVAILABLE,
                            severity = IssueSeverity.CRITICAL,
                            description = "Topic not found"
                        )
                    )
                )
            }

            val partitions = topicDetails.partitions()
            val underReplicatedPartitions = partitions.count { partition -> partition.isr().size < partition.replicas().size }
            val offlinePartitions = partitions.count { partition -> partition.leader() == null }
            val totalPartitions = partitions.size
            val replicationFactor = partitions.firstOrNull()?.replicas()?.size ?: 0
            val averageReplicationFactor = partitions.map { partition -> partition.replicas().size }.average()

            val issues = mutableListOf<TopicIssueDto>()

            if (underReplicatedPartitions > 0) {
                issues.add(TopicIssueDto(
                    type = IssueType.UNDER_REPLICATED,
                    severity = IssueSeverity.MEDIUM,
                    description = "$underReplicatedPartitions partitions are under-replicated"
                ))
            }

            if (offlinePartitions > 0) {
                issues.add(TopicIssueDto(
                    type = IssueType.OFFLINE_PARTITION,
                    severity = IssueSeverity.HIGH,
                    description = "$offlinePartitions partitions are offline"
                ))
            }

            val healthScore = when {
                offlinePartitions > 0 -> 0
                underReplicatedPartitions > 0 -> 50
                else -> 100
            }

            TopicHealthDto(
                topicName = topicName.value,
                isHealthy = healthScore > 0,
                healthScore = healthScore,
                underReplicatedPartitions = underReplicatedPartitions,
                offlinePartitions = offlinePartitions,
                totalPartitions = totalPartitions,
                replicationFactor = replicationFactor,
                averageReplicationFactor = averageReplicationFactor,
                lastUpdated = LocalDateTime.now(),
                issues = issues
            )
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getAllTopicsHealth(connection: Connection): List<TopicHealthDto> {
        val adminClient = createAdminClient(connection)
        return try {
            val topicList = adminClient.listTopics().names().get()
            topicList.map { topicName ->
                getTopicHealth(connection, TopicName(topicName))
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getConsumerGroupMetrics(connection: Connection, groupId: String): ConsumerGroupMetricsDto {
        val adminClient = createAdminClient(connection)
        return try {
            val groupDescription = adminClient.describeConsumerGroups(listOf(groupId)).all().get()[groupId]
            
            ConsumerGroupMetricsDto(
                groupId = groupId,
                state = groupDescription?.state()?.name ?: "UNKNOWN",
                memberCount = groupDescription?.members()?.size ?: 0,
                topicCount = 0,
                totalLag = 0,
                averageLag = 0.0,
                maxLag = 0,
                minLag = 0,
                lastCommitTime = null,
                partitions = emptyList(),
                members = emptyList()
            )
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getAllConsumerGroupMetrics(connection: Connection): List<ConsumerGroupMetricsDto> {
        val adminClient = createAdminClient(connection)
        return try {
            val consumerGroups = adminClient.listConsumerGroups().all().get()
            consumerGroups.map { groupListing ->
                getConsumerGroupMetrics(connection, groupListing.groupId())
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getPartitionDetails(connection: Connection, topicName: TopicName, partitionNumber: Int): PartitionDetailDto {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(topicName.value, partitionNumber)
            consumer.assign(listOf(topicPartition))
            
            val beginningOffsets = consumer.beginningOffsets(listOf(topicPartition))
            val endOffsets = consumer.endOffsets(listOf(topicPartition))
            
            val earliestOffset = beginningOffsets[topicPartition] ?: 0
            val latestOffset = endOffsets[topicPartition] ?: 0
            
            PartitionDetailDto(
                topicName = topicName.value,
                partitionNumber = partitionNumber,
                leader = 0,
                replicas = emptyList(),
                inSyncReplicas = emptyList(),
                earliestOffset = earliestOffset,
                latestOffset = latestOffset,
                messageCount = latestOffset - earliestOffset,
                isHealthy = true,
                isUnderReplicated = false,
                lastUpdated = LocalDateTime.now()
            )
        } finally {
            consumer.close()
        }
    }

    override suspend fun getOffsetInfo(connection: Connection, topicName: TopicName, partitionNumber: Int): OffsetInfoDto {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(topicName.value, partitionNumber)
            consumer.assign(listOf(topicPartition))
            
            val beginningOffsets = consumer.beginningOffsets(listOf(topicPartition))
            val endOffsets = consumer.endOffsets(listOf(topicPartition))
            
            val earliestOffset = beginningOffsets[topicPartition] ?: 0
            val latestOffset = endOffsets[topicPartition] ?: 0
            
            OffsetInfoDto(
                topicName = topicName.value,
                partitionNumber = partitionNumber,
                currentOffset = earliestOffset,
                committedOffset = earliestOffset,
                endOffset = latestOffset,
                lag = latestOffset - earliestOffset,
                consumerGroup = null,
                lastCommitTime = null
            )
        } finally {
            consumer.close()
        }
    }

    override suspend fun setOffset(connection: Connection, topicName: TopicName, partitionNumber: Int, offset: Long): Boolean {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(topicName.value, partitionNumber)
            consumer.assign(listOf(topicPartition))
            consumer.seek(topicPartition, offset)
            true
        } catch (e: Exception) {
            false
        } finally {
            consumer.close()
        }
    }

    override suspend fun getClusterInfo(connection: Connection): ClusterInfoDto {
        val adminClient = createAdminClient(connection)
        return try {
            val clusterDescription = adminClient.describeCluster().nodes().get()
            val controller = adminClient.describeCluster().controller().get()
            val topicList = adminClient.listTopics().names().get()

            ClusterInfoDto(
                clusterId = "cluster-${connection.getId().value}",
                controllerId = controller?.id() ?: -1,
                totalBrokers = clusterDescription.size,
                activeBrokers = clusterDescription.size,
                totalTopics = topicList.size,
                totalPartitions = 0,
                version = "3.9.0",
                lastUpdated = LocalDateTime.now()
            )
        } finally {
            adminClient.close()
        }
    }

    private fun createAdminClient(connection: Connection): AdminClient {
        val props = Properties()
        props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = connection.getConnectionString()
        props[AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG] = 5000
        props[AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = 5000

        if (connection.sslEnabled) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
            logger.info("SSL enabled for connection: ${connection.getConnectionString()}")
        }

        if (connection.saslEnabled && connection.username != null) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_PLAINTEXT"
            props["sasl.mechanism"] = "PLAIN"
            props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${connection.username}\" password=\"${connection.password}\";"
            logger.info("SASL enabled for connection: ${connection.getConnectionString()} with username: ${connection.username}")
        }

        logger.info("Creating AdminClient with properties: $props")
        return AdminClient.create(props)
    }

    private fun createConsumer(connection: Connection): KafkaConsumer<String, String> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = connection.getConnectionString()
        props[ConsumerConfig.GROUP_ID_CONFIG] = "kopanda-consumer"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false

        if (connection.sslEnabled) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        }

        if (connection.saslEnabled && connection.username != null) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_PLAINTEXT"
            props["sasl.mechanism"] = "PLAIN"
            props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${connection.username}\" password=\"${connection.password}\";"
        }

        return KafkaConsumer(props)
    }

    private fun createProducer(connection: Connection): KafkaProducer<String, String> {
        val props = Properties()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = connection.getConnectionString()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.ACKS_CONFIG] = "all"

        if (connection.sslEnabled) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        }

        if (connection.saslEnabled && connection.username != null) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_PLAINTEXT"
            props["sasl.mechanism"] = "PLAIN"
            props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${connection.username}\" password=\"${connection.password}\";"
        }

        return KafkaProducer(props)
    }
}