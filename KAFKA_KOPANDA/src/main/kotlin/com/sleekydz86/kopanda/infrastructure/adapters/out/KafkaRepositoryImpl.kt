package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto
import com.sleekydz86.kopanda.application.dto.response.BrokerMetricsDto
import com.sleekydz86.kopanda.application.dto.response.ClusterInfoDto
import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupDto
import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupMetricsDto
import com.sleekydz86.kopanda.application.dto.response.ConsumerMemberMetricsDto
import com.sleekydz86.kopanda.application.dto.response.DetailedMetricsDto
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto
import com.sleekydz86.kopanda.application.dto.response.OffsetInfoDto
import com.sleekydz86.kopanda.application.dto.response.PartitionLagDto
import com.sleekydz86.kopanda.application.dto.response.PartitionMetricsDto
import com.sleekydz86.kopanda.application.dto.response.PerformanceMetricsDto
import com.sleekydz86.kopanda.application.dto.response.TopicHealthDto
import com.sleekydz86.kopanda.application.dto.response.TopicIssueDto
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
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueType
import com.sleekydz86.kopanda.domain.valueobjects.topic.TopicConfig
import com.sleekydz86.kopanda.shared.domain.OffsetChangeConflictException
import com.sleekydz86.kopanda.domain.valueobjects.message.MessageKey
import com.sleekydz86.kopanda.domain.valueobjects.message.MessageValue
import com.sleekydz86.kopanda.domain.valueobjects.message.Timestamp
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.ListTopicsOptions
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.ConsumerGroupState
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.Node
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.TopicPartitionInfo
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.errors.GroupSubscribedToTopicException
import org.apache.kafka.common.errors.UnknownMemberIdException
import org.apache.kafka.common.errors.UnsupportedVersionException
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger

@Repository
class KafkaRepositoryImpl : KafkaRepository {
    
    private val logger = Logger.getLogger(KafkaRepositoryImpl::class.java.name)

    override suspend fun getTopics(connection: Connection): List<Topic> {
        val adminClient = createAdminClient(connection)
        return try {
            val topicList = adminClient.listTopics().names().get()

            val topics = topicList.mapNotNull { topicName ->
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
            fillOffsets(adminClient, topics)
            topics
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

            val topic = toTopic(topicName.value, toPartitions(topicDetails.partitions()))
            fillOffsets(adminClient, listOf(topic))
            topic
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
            val endOffset = consumer.endOffsets(listOf(topicPartition)).getValue(topicPartition)
            if (offset.value >= endOffset) {
                emptyList()
            } else {
                val messages = mutableListOf<Message>()
                val deadline = System.currentTimeMillis() + 10_000
                while (messages.size < limit &&
                    consumer.position(topicPartition) < endOffset &&
                    System.currentTimeMillis() < deadline
                ) {
                    val records = consumer.poll(Duration.ofMillis(500))
                    for (record in records.records(topicPartition)) {
                        if (messages.size >= limit) {
                            break
                        }
                        messages.add(toFetchedMessage(record))
                    }
                }
                messages
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
            val endOffset = consumer.endOffsets(listOf(topicPartition)).getValue(topicPartition)
            if (consumer.position(topicPartition) >= endOffset) {
                return emptyList()
            }

            val messages = mutableListOf<Message>()
            val deadline = System.currentTimeMillis() + 10_000
            while (messages.size < criteria.limit &&
                consumer.position(topicPartition) < endOffset &&
                System.currentTimeMillis() < deadline
            ) {
                val records = consumer.poll(Duration.ofMillis(500))
                for (record in records.records(topicPartition)) {
                    if (messages.size >= criteria.limit) {
                        break
                    }
                    val keyMatched = criteria.key == null || record.key()?.contains(criteria.key) == true
                    val valueMatched = criteria.value == null || record.value()?.contains(criteria.value) == true
                    if (keyMatched && valueMatched) {
                        messages.add(toFetchedMessage(record))
                    }
                }
            }

            messages
        } finally {
            consumer.close()
        }
    }

    private fun toFetchedMessage(record: ConsumerRecord<String, String>): Message {
        return Message.create(
            offset = record.offset(),
            key = record.key(),
            value = record.value() ?: "",
            timestamp = record.timestamp(),
            partitionNumber = record.partition(),
            headers = emptyMap()
        )
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
        return createAdminClient(connection).use { adminClient ->
            topicHealth(adminClient, topicName.value)
        }
    }

    override suspend fun getAllTopicsHealth(connection: Connection): List<TopicHealthDto> {
        return createAdminClient(connection).use { adminClient ->
            val listings = adminClient.listTopics(ListTopicsOptions().listInternal(true)).listings().get()
            listings.filter { !it.isInternal }.map { listing ->
                topicHealth(adminClient, listing.name())
            }
        }
    }

    override suspend fun getConsumerGroupMetrics(connection: Connection, groupId: String): ConsumerGroupMetricsDto {
        return createAdminClient(connection).use { adminClient ->
            consumerGroupMetrics(adminClient, groupId)
        }
    }

    override suspend fun getAllConsumerGroupMetrics(connection: Connection): List<ConsumerGroupMetricsDto> {
        return createAdminClient(connection).use { adminClient ->
            adminClient.listConsumerGroups().all().get().map { group ->
                consumerGroupMetrics(adminClient, group.groupId())
            }
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
        return createAdminClient(connection).use { adminClient ->
            val partitionInfo = partitionInfo(adminClient, topicName.value, partitionNumber)
            val offsets = readOffsets(adminClient, listOf(TopicPartition(topicName.value, partitionNumber)))
            val offset = offsets.getValue(TopicPartition(topicName.value, partitionNumber))
            val replicas = brokerIds(partitionInfo.replicas()).map { it.value }
            val inSyncReplicas = brokerIds(partitionInfo.isr()).map { it.value }
            val leader = partitionInfo.leader()?.takeIf { !it.isEmpty && it.id() >= 0 }
            val underReplicated = partitionInfo.isr().size < partitionInfo.replicas().size
            val earliest = offset.earliest
            val latest = offset.latest
            PartitionDetailDto(
                topicName = topicName.value,
                partitionNumber = partitionNumber,
                leader = leader?.id() ?: -1,
                replicas = replicas,
                inSyncReplicas = inSyncReplicas,
                earliestOffset = earliest,
                latestOffset = latest,
                messageCount = (latest - earliest).coerceAtLeast(0),
                isHealthy = leader != null && !underReplicated,
                isUnderReplicated = underReplicated,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    override suspend fun getOffsetInfo(connection: Connection, topicName: TopicName, partitionNumber: Int): OffsetInfoDto {
        return createAdminClient(connection).use { adminClient ->
            partitionInfo(adminClient, topicName.value, partitionNumber)
            val offsets = readOffsets(adminClient, listOf(TopicPartition(topicName.value, partitionNumber)))
            val offset = offsets.getValue(TopicPartition(topicName.value, partitionNumber))
            OffsetInfoDto(
                topicName = topicName.value,
                partitionNumber = partitionNumber,
                currentOffset = offset.earliest,
                committedOffset = null,
                endOffset = offset.latest,
                lag = null,
                consumerGroup = null,
                lastCommitTime = null
            )
        }
    }

    override suspend fun setOffset(
        connection: Connection,
        topicName: TopicName,
        partitionNumber: Int,
        groupId: String,
        offset: Long
    ): Boolean {
        return createAdminClient(connection).use { adminClient ->
            partitionInfo(adminClient, topicName.value, partitionNumber)
            val topicPartition = TopicPartition(topicName.value, partitionNumber)
            val range = readOffsets(adminClient, listOf(topicPartition)).getValue(topicPartition)
            if (offset < range.earliest || offset > range.latest) {
                throw IllegalArgumentException("오프셋이 허용 범위를 벗어났습니다: $offset")
            }
            try {
                adminClient.alterConsumerGroupOffsets(
                    groupId,
                    mapOf(topicPartition to OffsetAndMetadata(offset))
                ).all().get()
            } catch (e: ExecutionException) {
                if (generateSequence(e as Throwable) { it.cause }.any {
                        it is UnknownMemberIdException || it is GroupSubscribedToTopicException
                    }
                ) {
                    throw OffsetChangeConflictException("컨슈머 그룹에 실행 중인 소비자가 있어 오프셋을 바꿀 수 없습니다")
                }
                throw e
            }
            true
        }
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

    private fun topicHealth(adminClient: AdminClient, topicName: String): TopicHealthDto {
        val description = adminClient.describeTopics(listOf(topicName)).allTopicNames().get()[topicName]
            ?: throw KafkaException("토픽을 찾을 수 없습니다: $topicName")
        val partitions = description.partitions()
        var underReplicated = 0
        var offline = 0
        var healthy = 0
        partitions.forEach { info ->
            val leader = info.leader()
            val hasLeader = leader != null && !leader.isEmpty && leader.id() >= 0
            val missingReplicas = info.isr().size < info.replicas().size
            if (!hasLeader) {
                offline += 1
            }
            if (missingReplicas) {
                underReplicated += 1
            }
            if (hasLeader && !missingReplicas) {
                healthy += 1
            }
        }
        val total = partitions.size
        val replicaSizes = partitions.map { it.replicas().size }
        val issues = mutableListOf<TopicIssueDto>()
        if (underReplicated > 0) {
            issues += TopicIssueDto(
                type = IssueType.UNDER_REPLICATED,
                severity = IssueSeverity.MEDIUM,
                description = "복제 부족 파티션이 ${underReplicated}개 있습니다."
            )
        }
        if (offline > 0) {
            issues += TopicIssueDto(
                type = IssueType.OFFLINE_PARTITION,
                severity = IssueSeverity.HIGH,
                description = "리더가 없는 파티션이 ${offline}개 있습니다."
            )
        }
        return TopicHealthDto(
            topicName = topicName,
            isHealthy = underReplicated == 0 && offline == 0,
            healthScore = if (total == 0) 0 else healthy * 100 / total,
            underReplicatedPartitions = underReplicated,
            offlinePartitions = offline,
            totalPartitions = total,
            replicationFactor = replicaSizes.maxOrNull() ?: 0,
            averageReplicationFactor = if (replicaSizes.isEmpty()) 0.0 else replicaSizes.average(),
            lastUpdated = LocalDateTime.now(),
            issues = issues
        )
    }

    private fun partitionInfo(adminClient: AdminClient, topicName: String, partitionNumber: Int): TopicPartitionInfo {
        val description = adminClient.describeTopics(listOf(topicName)).allTopicNames().get()[topicName]
            ?: throw KafkaException("토픽을 찾을 수 없습니다: $topicName")
        return description.partitions().find { it.partition() == partitionNumber }
            ?: throw KafkaException("파티션을 찾을 수 없습니다: $topicName-$partitionNumber")
    }

    private fun consumerGroupMetrics(adminClient: AdminClient, groupId: String): ConsumerGroupMetricsDto {
        val description = adminClient.describeConsumerGroups(listOf(groupId)).all().get()[groupId]
            ?: throw KafkaException("컨슈머 그룹을 찾을 수 없습니다: $groupId")
        val committed = adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata(groupId).get()
        if (description.state() == ConsumerGroupState.DEAD && description.members().isEmpty() && committed.isEmpty()) {
            throw KafkaException("컨슈머 그룹을 찾을 수 없습니다: $groupId")
        }
        val latest = readOffsets(adminClient, committed.keys.toList())
        val memberByPartition = description.members().flatMap { member ->
            member.assignment().topicPartitions().map { topicPartition -> topicPartition to member.consumerId() }
        }.toMap()
        val partitions = committed.map { (topicPartition, metadata) ->
            val endOffset = latest[topicPartition]?.latest
                ?: throw KafkaException("최신 오프셋을 가져오지 못했습니다: ${topicPartition.topic()}-${topicPartition.partition()}")
            val committedOffset = metadata.offset()
            PartitionLagDto(
                topic = topicPartition.topic(),
                partition = topicPartition.partition(),
                currentOffset = committedOffset,
                committedOffset = committedOffset,
                endOffset = endOffset,
                lag = (endOffset - committedOffset).coerceAtLeast(0),
                consumerId = memberByPartition[topicPartition]
            )
        }
        val lags = partitions.map { it.lag }
        val members = description.members().map { member ->
            val assigned = member.assignment().topicPartitions()
            val memberLags = partitions.filter { lag ->
                assigned.any { it.topic() == lag.topic && it.partition() == lag.partition }
            }.map { it.lag }
            ConsumerMemberMetricsDto(
                memberId = member.consumerId(),
                clientId = member.clientId(),
                clientHost = member.host(),
                assignedPartitions = assigned.size,
                totalLag = memberLags.sum(),
                averageLag = if (memberLags.isEmpty()) 0.0 else memberLags.average(),
                lastHeartbeat = null
            )
        }
        return ConsumerGroupMetricsDto(
            groupId = groupId,
            state = description.state().toString(),
            memberCount = description.members().size,
            topicCount = committed.keys.map { it.topic() }.distinct().size,
            totalLag = lags.sum(),
            averageLag = if (lags.isEmpty()) 0.0 else lags.average(),
            maxLag = lags.maxOrNull() ?: 0L,
            minLag = lags.minOrNull() ?: 0L,
            lastCommitTime = null,
            partitions = partitions,
            members = members
        )
    }

    private fun fillOffsets(adminClient: AdminClient, topics: List<Topic>) {
        val indexed = topics.flatMap { topic ->
            topic.partitions.map { partition ->
                TopicPartition(topic.name.value, partition.partitionNumber.value) to partition
            }
        }
        if (indexed.isEmpty()) {
            return
        }
        try {
            val offsets = readOffsets(adminClient, indexed.map { it.first })
            indexed.forEach { (topicPartition, partition) ->
                val offset = offsets[topicPartition] ?: return@forEach
                partition.updateOffsetRange(offset.earliest, offset.latest)
                partition.updateMessageCount((offset.latest - offset.earliest).coerceAtLeast(0))
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "오프셋 조회에 실패했습니다", e)
        }
    }

    private fun readOffsets(adminClient: AdminClient, partitions: List<TopicPartition>): Map<TopicPartition, OffsetPair> {
        if (partitions.isEmpty()) {
            return emptyMap()
        }
        val earliest = adminClient.listOffsets(partitions.associateWith { OffsetSpec.earliest() }).all().get()
        val latest = adminClient.listOffsets(partitions.associateWith { OffsetSpec.latest() }).all().get()
        return partitions.associateWith { topicPartition ->
            OffsetPair(
                earliest = earliest.getValue(topicPartition).offset(),
                latest = latest.getValue(topicPartition).offset()
            )
        }
    }

    private data class OffsetPair(
        val earliest: Long,
        val latest: Long
    )

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
        val config = KafkaClientProps.base(connection)
        config[AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG] = 5000
        config[AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = 10000
        
        config[AdminClientConfig.RETRIES_CONFIG] = 3
        config[AdminClientConfig.RETRY_BACKOFF_MS_CONFIG] = 100
        config[AdminClientConfig.CLIENT_ID_CONFIG] = "kopanda-admin-client"
        
        config[AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG] = 300000 // 5분
        
        return AdminClient.create(config)
    }

    private fun createConsumer(connection: Connection): KafkaConsumer<String, String> {
        val config = KafkaClientProps.base(connection)
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return KafkaConsumer(config)
    }

    private fun createProducer(connection: Connection): KafkaProducer<String, String> {
        val config = KafkaClientProps.base(connection)
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        return KafkaProducer(config)
    }
}