package com.sleekydz86.kopanda.infrastructure.adapters.out

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
import com.sleekydz86.kopanda.application.ports.out.KafkaRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Message
import com.sleekydz86.kopanda.domain.entities.Partition
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
import org.apache.kafka.clients.admin.ListTopicsOptions
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.Node
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.TopicPartitionInfo
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.errors.UnsupportedVersionException
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Logger

@Repository
class KafkaRepositoryImpl : KafkaRepository {
    
    private val logger = Logger.getLogger(KafkaRepositoryImpl::class.java.name)

    override suspend fun getTopics(connection: Connection): List<Topic> {
        val adminClient = createAdminClient(connection)
        return try {
            val topicList = adminClient.listTopics().names().get()
            
            topicList.mapNotNull { topicName ->
                try {
                    val topicDetails = adminClient.describeTopics(listOf(topicName)).all().get()[topicName]
                    val partitions = topicDetails?.partitions()?.let { toPartitions(it) } ?: emptyList()

                    toTopic(topicName, partitions)
                } catch (e: Exception) {
                    val cause = generateSequence<Throwable>(e) { it.cause }
                        .firstOrNull { it is UnsupportedVersionException }
                    if (cause != null) {
                        logger.warning("Kafka 버전 호환성 문제로 토픽 상세 정보를 가져올 수 없습니다: $topicName")
                    } else {
                        logger.warning("토픽 상세 정보 조회 실패: $topicName - ${e.message}")
                    }

                    try {
                        toTopic(topicName, emptyList(), fallback = true)
                    } catch (buildError: Exception) {
                        logger.warning("토픽을 목록에서 제외합니다: $topicName - ${buildError.message}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is UnsupportedVersionException -> {
                    logger.severe("Kafka 버전 호환성 문제로 토픽 목록을 가져올 수 없습니다: ${e.message}")
                }
                else -> {
                    logger.severe("토픽 목록 조회 실패: ${e.message}")
                }
            }
            emptyList()
        } finally {
            adminClient.close()
        }
    }

    override suspend fun getTopicDetails(connection: Connection, topicName: TopicName): Topic? {
        val adminClient = createAdminClient(connection)
        return try {
            val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]
            if (topicDetails == null) return null

            toTopic(topicName.value, toPartitions(topicDetails.partitions()))
        } catch (e: Exception) {
            logger.warning("토픽 상세 정보 조회 실패: ${topicName.value} - ${e.message}")
            try {
                toTopic(topicName.value, emptyList(), fallback = true)
            } catch (buildError: Exception) {
                logger.warning("토픽 상세 정보를 만들 수 없습니다: ${topicName.value} - ${buildError.message}")
                null
            }
        } finally {
            adminClient.close()
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
        partition: Partition,
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
        val snapshot = snapshot(connection)
        return KafkaMetricsDto(
            brokerCount = snapshot.brokers,
            topicCount = snapshot.topics,
            totalPartitions = snapshot.partitions,
            messagesPerSecond = null,
            bytesInPerSec = null,
            bytesOutPerSec = null,
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

    override suspend fun getDetailedMetrics(connection: Connection): DetailedMetricsDto {
        return DetailedMetricsDto(
            connectionId = connection.getId().value,
            brokerMetrics = BrokerMetricsDto(
                totalBrokers = 1,
                activeBrokers = 1,
                offlineBrokers = 0,
                averageResponseTime = 0L,
                totalDiskUsage = 0L,
                availableDiskSpace = 0L
            ),
            topicMetrics = TopicMetricsDto(
                totalTopics = 0,
                internalTopics = 0,
                userTopics = 0,
                totalPartitions = 0,
                underReplicatedPartitions = 0,
                offlinePartitions = 0,
                totalMessages = 0L,
                messagesPerSecond = 0.0
            ),
            partitionMetrics = PartitionMetricsDto(
                totalPartitions = 0,
                healthyPartitions = 0,
                underReplicatedPartitions = 0,
                offlinePartitions = 0,
                averageReplicationFactor = 0.0,
                totalMessages = 0L,
                averageMessagesPerPartition = 0.0
            ),
            performanceMetrics = PerformanceMetricsDto(
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

    override suspend fun getTopicHealth(connection: Connection, topicName: TopicName): TopicHealthDto {
        return TopicHealthDto(
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

    override suspend fun getAllTopicsHealth(connection: Connection): List<TopicHealthDto> {
        return emptyList()
    }

    override suspend fun getConsumerGroupMetrics(connection: Connection, groupId: String): ConsumerGroupMetricsDto {
        return ConsumerGroupMetricsDto(
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

    override suspend fun getAllConsumerGroupMetrics(connection: Connection): List<ConsumerGroupMetricsDto> {
        return emptyList()
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
        return PartitionDetailDto(
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

    override suspend fun getOffsetInfo(connection: Connection, topicName: TopicName, partitionNumber: Int): OffsetInfoDto {
        return OffsetInfoDto(
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

    override suspend fun getClusterInfo(connection: Connection): ClusterInfoDto {
        val snapshot = snapshot(connection)
        return ClusterInfoDto(
            clusterId = snapshot.clusterId,
            controllerId = snapshot.controllerId,
            totalBrokers = snapshot.brokers,
            activeBrokers = snapshot.brokers,
            totalTopics = snapshot.topics,
            totalPartitions = snapshot.partitions,
            version = null,
            lastUpdated = LocalDateTime.now()
        )
    }

    override suspend fun getBrokerMetrics(connection: Connection): BrokerMetricsDto {
        val snapshot = snapshot(connection)
        return BrokerMetricsDto(
            totalBrokers = snapshot.brokers,
            activeBrokers = snapshot.brokers,
            offlineBrokers = 0,
            averageResponseTime = 0L,
            totalDiskUsage = 0L,
            availableDiskSpace = 0L
        )
    }

    override suspend fun getTopicMetrics(connection: Connection): TopicMetricsDto {
        val snapshot = snapshot(connection)
        return TopicMetricsDto(
            totalTopics = snapshot.topics,
            internalTopics = snapshot.internalTopics,
            userTopics = snapshot.topics,
            totalPartitions = snapshot.partitions,
            underReplicatedPartitions = snapshot.underReplicated,
            offlinePartitions = snapshot.offline,
            totalMessages = 0L,
            messagesPerSecond = null
        )
    }

    override suspend fun getPartitionMetrics(connection: Connection): PartitionMetricsDto {
        val snapshot = snapshot(connection)
        return PartitionMetricsDto(
            totalPartitions = snapshot.partitions,
            healthyPartitions = snapshot.healthyPartitions,
            underReplicatedPartitions = snapshot.underReplicated,
            offlinePartitions = snapshot.offline,
            averageReplicationFactor = snapshot.averageReplicationFactor,
            totalMessages = 0L,
            averageMessagesPerPartition = null
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

    private fun toPartitions(
        partitionInfos: List<TopicPartitionInfo>
    ): List<Partition> {
        return partitionInfos.mapNotNull { partitionInfo ->
            val leaderId = partitionInfo.leader()?.id()?.takeIf { it >= 0 }
                ?: partitionInfo.replicas().firstOrNull { it.id() >= 0 }?.id()
                ?: return@mapNotNull null

            Partition(
                partitionNumber = PartitionNumber(partitionInfo.partition()),
                leader = BrokerId(leaderId),
                replicas = brokerIds(partitionInfo.replicas()),
                inSyncReplicas = brokerIds(partitionInfo.isr()),
                earliestOffset = 0,
                latestOffset = 0
            )
        }
    }

    private fun brokerIds(nodes: List<Node>): List<BrokerId> {
        return nodes.mapNotNull { node -> node.id().takeIf { it >= 0 }?.let(::BrokerId) }
    }

    private fun toTopic(
        topicName: String,
        partitions: List<Partition>,
        fallback: Boolean = false
    ): Topic {
        val name = TopicName(topicName)
        return Topic(
            name = name,
            config = TopicConfig(
                partitionCount = partitions.size.takeIf { !fallback && it > 0 } ?: 1,
                replicationFactor = partitions.firstOrNull()?.replicas?.size?.takeIf { it > 0 } ?: 1,
                config = emptyMap()
            ),
            isInternal = name.isInternal
        ).apply {
            partitions.forEach { addPartition(it) }
        }
    }

    private data class ClusterSnapshot(
        val clusterId: String,
        val controllerId: Int,
        val brokers: Int,
        val topics: Int,
        val internalTopics: Int,
        val partitions: Int,
        val underReplicated: Int,
        val offline: Int,
        val healthyPartitions: Int,
        val averageReplicationFactor: Double
    )

    private fun snapshot(connection: Connection): ClusterSnapshot =
        createAdminClient(connection).use { admin ->
            val described = admin.describeCluster()
            val clusterId = described.clusterId().get()
                ?: throw KafkaException("클러스터 ID를 가져오지 못했습니다")
            val controllerId = described.controller().get()?.id()?.takeIf { it >= 0 } ?: -1
            val brokers = described.nodes().get().size
            val listings = admin.listTopics(ListTopicsOptions().listInternal(true)).listings().get()
            val userNames = listings.filter { !it.isInternal }.map { it.name() }
            val descriptions = if (userNames.isEmpty()) {
                emptyMap()
            } else {
                admin.describeTopics(userNames).allTopicNames().get()
            }
            val parts = descriptions.values.flatMap { it.partitions() }
            val underReplicated = parts.count { it.isr().size < it.replicas().size }
            val offline = parts.count { part ->
                val leader = part.leader()
                leader == null || leader.isEmpty
            }
            val healthyPartitions = parts.count { part ->
                val leader = part.leader()
                leader != null && !leader.isEmpty && part.isr().size >= part.replicas().size
            }
            val averageReplicationFactor = if (parts.isEmpty()) {
                0.0
            } else {
                parts.map { it.replicas().size }.average()
            }
            ClusterSnapshot(
                clusterId = clusterId,
                controllerId = controllerId,
                brokers = brokers,
                topics = userNames.size,
                internalTopics = listings.count { it.isInternal },
                partitions = parts.size,
                underReplicated = underReplicated,
                offline = offline,
                healthyPartitions = healthyPartitions,
                averageReplicationFactor = averageReplicationFactor
            )
        }

    private fun createAdminClient(connection: Connection): AdminClient {
        val config = Properties()
        config[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = "${connection.host.value}:${connection.port.value}"
        config[AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG] = 5000
        config[AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = 10000
        
        config[AdminClientConfig.RETRIES_CONFIG] = 3
        config[AdminClientConfig.RETRY_BACKOFF_MS_CONFIG] = 100
        config[AdminClientConfig.CLIENT_ID_CONFIG] = "kopanda-admin-client"
        
        config[AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG] = 300000 // 5분
        
        return AdminClient.create(config)
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