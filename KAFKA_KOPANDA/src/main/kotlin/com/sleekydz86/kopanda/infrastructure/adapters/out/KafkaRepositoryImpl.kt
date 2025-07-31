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
        val consumer = createConsumer(connection)

        return try {
            val topicNames = adminClient.listTopics().names().get()
            topicNames.filter { !it.startsWith("__") }.map { topicName ->
                getTopicDetails(connection, TopicName(topicName)) ?: createDefaultTopic(topicName)
            }
        } finally {
            adminClient.close()
            consumer.close()
        }
    }

    private fun createDefaultTopic(topicName: String): Topic {
        return Topic(
            name = TopicName(topicName),
            config = TopicConfig(
                partitionCount = 1,
                replicationFactor = 1,
                config = emptyMap()
            )
        ).apply {
            addPartition(
                com.sleekydz86.kopanda.domain.entities.Partition(
                    partitionNumber = PartitionNumber(0),
                    leader = BrokerId(0),
                    replicas = listOf(BrokerId(0)),
                    inSyncReplicas = listOf(BrokerId(0)),
                    earliestOffset = 0L,
                    latestOffset = 0L
                )
            )
        }
    }

    override suspend fun getTopicDetails(connection: Connection, topicName: TopicName): Topic? {
        val adminClient = createAdminClient(connection)
        val consumer = createConsumer(connection)
        return try {
            val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]
            if (topicDetails == null) return null

            val partitions = topicDetails.partitions().map { partitionInfo ->
                val topicPartition = TopicPartition(topicName.value, partitionInfo.partition())

                var earliestOffset = 0L
                var latestOffset = 0L

                try {
                    consumer.assign(listOf(topicPartition))
                    val beginningOffsets = consumer.beginningOffsets(listOf(topicPartition))
                    val endOffsets = consumer.endOffsets(listOf(topicPartition))
                    earliestOffset = beginningOffsets[topicPartition] ?: 0L
                    latestOffset = endOffsets[topicPartition] ?: 0L
                } catch (e: Exception) {
                    logger.warn("오프셋 정보 조회 실패: ${topicName.value}:${partitionInfo.partition()}", e)
                }

                com.sleekydz86.kopanda.domain.entities.Partition(
                    partitionNumber = PartitionNumber(partitionInfo.partition()),
                    leader = BrokerId(partitionInfo.leader().id()),
                    replicas = partitionInfo.replicas().map { broker -> BrokerId(broker.id()) },
                    inSyncReplicas = partitionInfo.isr().map { broker -> BrokerId(broker.id()) },
                    earliestOffset = earliestOffset,
                    latestOffset = latestOffset
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
            consumer.close()
        }
    }

    override suspend fun createTopic(connection: Connection, topic: Topic): Topic {
        val adminClient = createAdminClient(connection)
        return try {
            val newTopic = NewTopic(topic.name.value, topic.config.partitionCount, topic.config.replicationFactor.toShort())
            newTopic.configs(topic.config.config)

            adminClient.createTopics(listOf(newTopic)).all().get()
            logger.info("토픽 생성 성공: ${topic.name.value}")
            topic
        } finally {
            adminClient.close()
        }
    }

    override suspend fun deleteTopic(connection: Connection, topicName: TopicName) {
        val adminClient = createAdminClient(connection)
        try {
            adminClient.deleteTopics(listOf(topicName.value)).all().get()
            logger.info("토픽 삭제 성공: ${topicName.value}")
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
                Message(
                    offset = Offset(record.offset()),
                    key = record.key()?.let { com.sleekydz86.kopanda.domain.valueobjects.message.MessageKey(it) },
                    value = com.sleekydz86.kopanda.domain.valueobjects.message.MessageValue(record.value() ?: ""),
                    timestamp = com.sleekydz86.kopanda.domain.valueobjects.message.Timestamp(record.timestamp()),
                    partitionNumber = partition.partitionNumber,
                    consumed = false,
                    headers = record.headers().associate { it.key() to String(it.value()) }
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
        return try {
            val record = ProducerRecord(topic.name.value, partition?.value, key, value)
            headers.forEach { (k, v) -> record.headers().add(k, v.toByteArray()) }

            producer.send(record).get()
            logger.info("메시지 전송 성공: ${topic.name.value}")
        } finally {
            producer.close()
        }
    }

    override suspend fun searchMessages(connection: Connection, criteria: MessageSearchCriteria): List<Message> {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(criteria.topic, criteria.partition ?: 0)
            consumer.assign(listOf(topicPartition))

            val startOffset = criteria.startOffset ?: consumer.beginningOffsets(listOf(topicPartition))[topicPartition] ?: 0L
            val endOffset = criteria.endOffset ?: consumer.endOffsets(listOf(topicPartition))[topicPartition] ?: 0L

            consumer.seek(topicPartition, startOffset)

            val messages = mutableListOf<Message>()
            var currentOffset = startOffset

            while (currentOffset < endOffset && messages.size < criteria.limit) {
                val records = consumer.poll(Duration.ofSeconds(1))
                val recordList = records.records(topicPartition)

                for (record in recordList) {
                    if (record.offset() >= endOffset) break

                    val message = Message(
                        offset = Offset(record.offset()),
                        key = record.key()?.let { com.sleekydz86.kopanda.domain.valueobjects.message.MessageKey(it) },
                        value = com.sleekydz86.kopanda.domain.valueobjects.message.MessageValue(record.value() ?: ""),
                        timestamp = com.sleekydz86.kopanda.domain.valueobjects.message.Timestamp(record.timestamp()),
                        partitionNumber = PartitionNumber(record.partition()),
                        consumed = false,
                        headers = record.headers().associate { it.key() to String(it.value()) }
                    )

                    if (criteria.key != null && record.key() != criteria.key) continue
                    if (criteria.value != null && !record.value().contains(criteria.value)) continue
                    if (criteria.startTime != null && record.timestamp() < criteria.startTime) continue
                    if (criteria.endTime != null && record.timestamp() > criteria.endTime) continue

                    messages.add(message)
                    if (messages.size >= criteria.limit) break
                }

                currentOffset = recordList.lastOrNull()?.offset()?.plus(1) ?: currentOffset
            }

            messages
        } finally {
            consumer.close()
        }
    }

    override suspend fun getConsumerGroups(connection: Connection): List<ConsumerGroupDto> {
        val adminClient = createAdminClient(connection)
        return try {
            val consumerGroupListings = adminClient.listConsumerGroups().all().get()
            consumerGroupListings.map { groupListing ->
                try {
                    val groupId = groupListing.groupId()
                    val groupDescription = adminClient.describeConsumerGroups(listOf(groupId)).all().get()[groupId]
                    ConsumerGroupDto(
                        groupId = groupId,
                        state = groupDescription?.state()?.toString() ?: "UNKNOWN",
                        memberCount = groupDescription?.members()?.size ?: 0,
                        topicCount = 0,
                        offsets = emptyMap()
                    )
                } catch (e: Exception) {
                    logger.warn("컨슈머 그룹 정보 조회 실패: ${groupListing.groupId()}", e)
                    ConsumerGroupDto(
                        groupId = groupListing.groupId(),
                        state = "UNKNOWN",
                        memberCount = 0,
                        topicCount = 0,
                        offsets = emptyMap()
                    )
                }
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getMetrics(connection: Connection): KafkaMetricsDto {
        return try {
            val brokerMetrics = getBrokerMetrics(connection)
            val topicMetrics = getTopicMetrics(connection)

            KafkaMetricsDto(
                brokerCount = brokerMetrics.totalBrokers,
                topicCount = topicMetrics.totalTopics,
                totalPartitions = topicMetrics.totalPartitions,
                messagesPerSecond = 0.0,
                bytesInPerSec = 0.0,
                bytesOutPerSec = 0.0,
                activeConnections = brokerMetrics.activeBrokers,
                timestamp = LocalDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("메트릭 조회 실패", e)
            KafkaMetricsDto(
                brokerCount = 0,
                topicCount = 0,
                totalPartitions = 0,
                messagesPerSecond = 0.0,
                bytesInPerSec = 0.0,
                bytesOutPerSec = 0.0,
                activeConnections = 0,
                timestamp = LocalDateTime.now()
            )
        }
    }

    override suspend fun testConnection(connection: Connection): Boolean {
        val adminClient = createAdminClient(connection)
        return try {
            adminClient.listTopics().names().get()
            true
        } catch (e: Exception) {
            logger.error("연결 테스트 실패: ${connection.name.value}", e)
            false
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getDetailedMetrics(connection: Connection): DetailedMetricsDto {
        val brokerMetrics = getBrokerMetrics(connection)
        val topicMetrics = getTopicMetrics(connection)
        val partitionMetrics = getPartitionMetrics(connection)
        val performanceMetrics = getPerformanceMetrics(connection)

        return DetailedMetricsDto(
            connectionId = connection.getId().value,
            brokerMetrics = brokerMetrics,
            topicMetrics = topicMetrics,
            partitionMetrics = partitionMetrics,
            performanceMetrics = performanceMetrics,
            timestamp = LocalDateTime.now()
        )
    }

    override suspend fun getTopicHealth(connection: Connection, topicName: TopicName): TopicHealthDto {
        val adminClient = createAdminClient(connection)
        return try {
            val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]
            if (topicDetails == null) {
                throw IllegalArgumentException("Topic not found: ${topicName.value}")
            }

            val partitions = topicDetails.partitions()
            val underReplicatedPartitions = partitions.count { it.isr().size < it.replicas().size }
            val offlinePartitions = partitions.count { it.leader().id() == -1 }

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

            TopicHealthDto(
                topicName = topicName.value,
                isHealthy = issues.isEmpty(),
                healthScore = if (issues.isEmpty()) 100 else 50,
                underReplicatedPartitions = underReplicatedPartitions,
                offlinePartitions = offlinePartitions,
                totalPartitions = partitions.size,
                replicationFactor = partitions.firstOrNull()?.replicas()?.size ?: 1,
                averageReplicationFactor = partitions.map { it.replicas().size }.average(),
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
            val topics = adminClient.listTopics().names().get()
            topics.map { topicName ->
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
            if (groupDescription == null) {
                throw IllegalArgumentException("Consumer group not found: $groupId")
            }

            ConsumerGroupMetricsDto(
                groupId = groupId,
                state = groupDescription.state().toString(),
                memberCount = groupDescription.members().size,
                topicCount = 0,
                totalLag = 0L,
                averageLag = 0.0,
                maxLag = 0L,
                minLag = 0L,
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
            val consumerGroupListings = adminClient.listConsumerGroups().all().get()
            consumerGroupListings.map { groupListing ->
                getConsumerGroupMetrics(connection, groupListing.groupId())
            }
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getPerformanceMetrics(connection: Connection): PerformanceMetricsDto {
        return PerformanceMetricsDto(
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

    override suspend fun getPartitionDetails(connection: Connection, topicName: TopicName, partitionNumber: Int): PartitionDetailDto {
        val adminClient = createAdminClient(connection)
        val consumer = createConsumer(connection)
        return try {
            val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]
            if (topicDetails == null) {
                throw IllegalArgumentException("Topic not found: ${topicName.value}")
            }

            val partitionInfo = topicDetails.partitions().find { it.partition() == partitionNumber }
            if (partitionInfo == null) {
                throw IllegalArgumentException("Partition not found: $partitionNumber")
            }

            val topicPartition = TopicPartition(topicName.value, partitionNumber)
            consumer.assign(listOf(topicPartition))
            val earliestOffset = consumer.beginningOffsets(listOf(topicPartition))[topicPartition] ?: 0L
            val latestOffset = consumer.endOffsets(listOf(topicPartition))[topicPartition] ?: 0L

            PartitionDetailDto(
                topicName = topicName.value,
                partitionNumber = partitionNumber,
                leader = partitionInfo.leader().id(),
                replicas = partitionInfo.replicas().map { it.id() },
                inSyncReplicas = partitionInfo.isr().map { it.id() },
                earliestOffset = earliestOffset,
                latestOffset = latestOffset,
                messageCount = latestOffset - earliestOffset,
                isHealthy = partitionInfo.leader().id() != -1,
                isUnderReplicated = partitionInfo.isr().size < partitionInfo.replicas().size,
                lastUpdated = LocalDateTime.now()
            )
        } finally {
            adminClient.close()
            consumer.close()
        }
    }

    override suspend fun getOffsetInfo(connection: Connection, topicName: TopicName, partitionNumber: Int): OffsetInfoDto {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(topicName.value, partitionNumber)
            consumer.assign(listOf(topicPartition))
            val currentOffset = consumer.position(topicPartition)
            val committedOffset = consumer.committed(setOf(topicPartition))[topicPartition]?.offset() ?: 0L
            val endOffset = consumer.endOffsets(listOf(topicPartition))[topicPartition] ?: 0L

            OffsetInfoDto(
                topicName = topicName.value,
                partitionNumber = partitionNumber,
                currentOffset = currentOffset,
                committedOffset = committedOffset,
                endOffset = endOffset,
                lag = endOffset - committedOffset,
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
            logger.error("오프셋 설정 실패: ${topicName.value}:$partitionNumber", e)
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
            val topics = adminClient.listTopics().names().get()

            ClusterInfoDto(
                clusterId = "unknown",
                controllerId = controller?.id() ?: -1,
                totalBrokers = clusterDescription.size,
                activeBrokers = clusterDescription.size,
                totalTopics = topics.size,
                totalPartitions = 0,
                version = "unknown",
                lastUpdated = LocalDateTime.now()
            )
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getBrokerMetrics(connection: Connection): BrokerMetricsDto {
        val adminClient = createAdminClient(connection)
        return try {
            val clusterDescription = adminClient.describeCluster().nodes().get()

            BrokerMetricsDto(
                totalBrokers = clusterDescription.size,
                activeBrokers = clusterDescription.size,
                offlineBrokers = 0,
                averageResponseTime = 0L,
                totalDiskUsage = 0L,
                availableDiskSpace = 0L
            )
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getTopicMetrics(connection: Connection): TopicMetricsDto {
        val adminClient = createAdminClient(connection)
        return try {
            val topics = adminClient.listTopics().names().get()
            val internalTopics = topics.count { it.startsWith("__") }
            val userTopics = topics.size - internalTopics

            var totalPartitions = 0
            var underReplicatedPartitions = 0
            var offlinePartitions = 0

            for (topicName in topics) {
                try {
                    val topicDetails = adminClient.describeTopics(listOf(topicName)).all().get()[topicName]
                    if (topicDetails != null) {
                        val partitions = topicDetails.partitions()
                        totalPartitions += partitions.size

                        for (partition in partitions) {
                            if (partition.isr().size < partition.replicas().size) {
                                underReplicatedPartitions++
                            }
                            if (partition.leader().id() == -1) {
                                offlinePartitions++
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("토픽 $topicName 파티션 정보 조회 실패", e)
                }
            }

            TopicMetricsDto(
                totalTopics = topics.size,
                internalTopics = internalTopics,
                userTopics = userTopics,
                totalPartitions = totalPartitions,
                underReplicatedPartitions = underReplicatedPartitions,
                offlinePartitions = offlinePartitions,
                totalMessages = 0L,
                messagesPerSecond = 0.0
            )
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getPartitionMetrics(connection: Connection): PartitionMetricsDto {
        return PartitionMetricsDto(
            totalPartitions = 0,
            healthyPartitions = 0,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            averageReplicationFactor = 1.0,
            totalMessages = 0L,
            averageMessagesPerPartition = 0.0
        )
    }

    override suspend fun createTestConsumerGroup(connection: Connection, topicName: String): ConsumerGroupDto {
        val consumer = createConsumer(connection).apply {
            val groupId = "test-consumer-group-${System.currentTimeMillis()}"
            val props = Properties()
            props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
            props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            props[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = "1000"

            subscribe(listOf(topicName))

            repeat(5) {
                val records = poll(Duration.ofMillis(100))
                records.forEach { record ->
                    logger.info("테스트 메시지 소비: ${record.value()}")
                }
            }

            close()
        }

        return getConsumerGroups(connection).find {
            it.groupId.startsWith("test-consumer-group-")
        } ?: ConsumerGroupDto(
            groupId = "test-consumer-group",
            state = "STABLE",
            memberCount = 1,
            topicCount = 1,
            offsets = mapOf(topicName to 0L)
        )
    }

    private fun createAdminClient(connection: Connection): AdminClient {
        val props = Properties()
        props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = connection.getConnectionString()
        props[AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG] = 5000
        props[AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = 5000

        if (connection.sslEnabled) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        }

        if (connection.saslEnabled && connection.username != null) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_PLAINTEXT"
            props["sasl.mechanism"] = "PLAIN"
            props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${connection.username}\" password=\"${connection.password}\";"
        }

        return AdminClient.create(props)
    }

    private fun createConsumer(connection: Connection): KafkaConsumer<String, String> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = connection.getConnectionString()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[ConsumerConfig.GROUP_ID_CONFIG] = "kopanda-consumer-${System.currentTimeMillis()}"

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
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

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