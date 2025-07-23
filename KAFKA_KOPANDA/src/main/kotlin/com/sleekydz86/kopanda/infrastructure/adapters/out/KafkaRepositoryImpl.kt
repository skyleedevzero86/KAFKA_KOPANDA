package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.application.dto.ConsumerGroupDto
import com.sleekydz86.kopanda.application.dto.KafkaMetricsDto
import com.sleekydz86.kopanda.application.dto.MessageSearchCriteria
import com.sleekydz86.kopanda.application.ports.out.KafkaRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Message
import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.domain.valueobjects.Offset
import com.sleekydz86.kopanda.domain.valueobjects.PartitionNumber
import com.sleekydz86.kopanda.domain.valueobjects.TopicName
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
                        partitionNumber = com.sleekydz86.kopanda.domain.valueobjects.PartitionNumber(partitionInfo.partition()),
                        leader = com.sleekydz86.kopanda.domain.valueobjects.BrokerId(partitionInfo.leader().id()),
                        replicas = partitionInfo.replicas().map { com.sleekydz86.kopanda.domain.valueobjects.BrokerId(it.id()) },
                        inSyncReplicas = partitionInfo.isr().map { com.sleekydz86.kopanda.domain.valueobjects.BrokerId(it.id()) },
                        earliestOffset = 0,
                        latestOffset = 0
                    )
                } ?: emptyList()

                Topic(
                    name = TopicName(topicName),
                    config = com.sleekydz86.kopanda.domain.valueobjects.TopicConfig(
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
        return try {
            val topicDetails = adminClient.describeTopics(listOf(topicName.value)).all().get()[topicName.value]
            if (topicDetails == null) return null

            val partitions = topicDetails.partitions().map { partitionInfo ->
                com.sleekydz86.kopanda.domain.entities.Partition(
                    partitionNumber = com.sleekydz86.kopanda.domain.valueobjects.PartitionNumber(partitionInfo.partition()),
                    leader = com.sleekydz86.kopanda.domain.valueobjects.BrokerId(partitionInfo.leader().id()),
                    replicas = partitionInfo.replicas().map { com.sleekydz86.kopanda.domain.valueobjects.BrokerId(it.id()) },
                    inSyncReplicas = partitionInfo.isr().map { com.sleekydz86.kopanda.domain.valueobjects.BrokerId(it.id()) },
                    earliestOffset = 0,
                    latestOffset = 0
                )
            }

            Topic(
                name = topicName,
                config = com.sleekydz86.kopanda.domain.valueobjects.TopicConfig(
                    partitionCount = partitions.size,
                    replicationFactor = partitions.firstOrNull()?.replicas?.size ?: 1,
                    config = emptyMap()
                )
            ).apply {
                partitions.forEach { addPartition(it) }
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
        partition: com.sleekydz86.kopanda.domain.entities.Partition,
        offset: Offset,
        limit: Int
    ): List<Message> {
        val consumer = createConsumer(connection)
        return try {
            val topicPartition = TopicPartition(topic.name.value, partition.partitionNumber.value)
            consumer.assign(listOf(topicPartition))
            consumer.seek(topicPartition, offset.value)

            val records = consumer.poll(Duration.ofSeconds(10))
            records.records(topicPartition).take(limit).map { record ->
                Message.create(
                    offset = record.offset(),
                    key = record.key(),
                    value = record.value() ?: "",
                    timestamp = record.timestamp(),
                    partitionNumber = record.partition(),
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
        try {
            val record = ProducerRecord(
                topic.name.value,
                partition?.value,
                key,
                value
            )

            headers.forEach { (k, v) ->
                record.headers().add(k, v.toByteArray())
            }

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

            val startOffset = when {
                criteria.startOffset != null -> criteria.startOffset
                criteria.startTime != null -> {
                    val timestamp = criteria.startTime
                    consumer.offsetsForTimes(mapOf(topicPartition to timestamp))[topicPartition]?.offset() ?: 0
                }
                else -> 0
            }

            consumer.seek(topicPartition, startOffset)

            val records = consumer.poll(Duration.ofSeconds(10))
            records.records(topicPartition).take(criteria.limit).map { record ->
                Message.create(
                    offset = record.offset(),
                    key = record.key(),
                    value = record.value() ?: "",
                    timestamp = record.timestamp(),
                    partitionNumber = record.partition(),
                    headers = record.headers().associate { it.key() to String(it.value()) }
                )
            }.filter { message ->
                (criteria.key == null || message.key?.value?.contains(criteria.key) == true) &&
                        (criteria.value == null || message.value.value.contains(criteria.value))
            }
        } finally {
            consumer.close()
        }
    }

    override suspend fun getConsumerGroups(connection: Connection): List<ConsumerGroupDto> {
        val adminClient = createAdminClient(connection)
        return try {
            val consumerGroups = adminClient.listConsumerGroups().all().get()
            consumerGroups.map { group ->
                val groupDescription = adminClient.describeConsumerGroups(listOf(group.groupId())).all().get()[group.groupId()]
                ConsumerGroupDto(
                    groupId = group.groupId(),
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
            adminClient.listTopics().names().get()
            true
        } catch (e: Exception) {
            false
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
            props["security.protocol"] = "SSL"
        }

        if (connection.saslEnabled && connection.username != null) {
            props["security.protocol"] = "SASL_PLAINTEXT"
            props["sasl.mechanism"] = "PLAIN"
            props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${connection.username}\" password=\"${connection.password}\";"
        }

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
            props["security.protocol"] = "SSL"
        }

        if (connection.saslEnabled && connection.username != null) {
            props["security.protocol"] = "SASL_PLAINTEXT"
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
            props["security.protocol"] = "SSL"
        }

        if (connection.saslEnabled && connection.username != null) {
            props["security.protocol"] = "SASL_PLAINTEXT"
            props["sasl.mechanism"] = "PLAIN"
            props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${connection.username}\" password=\"${connection.password}\";"
        }

        return KafkaProducer(props)
    }
}