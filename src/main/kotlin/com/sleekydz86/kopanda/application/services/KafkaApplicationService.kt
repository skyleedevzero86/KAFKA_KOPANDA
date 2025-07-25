package com.sleekydz86.kopanda.application.services

<<<<<<< HEAD
import com.sleekydz86.kopanda.application.dto.common.BrokerInfo
import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.common.ConnectionTestResult
import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.application.dto.common.OffsetType
import com.sleekydz86.kopanda.application.dto.common.PartitionDto
import com.sleekydz86.kopanda.application.dto.enums.ConnectionStatusType
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.CreateTopicRequest
import com.sleekydz86.kopanda.application.dto.request.SendMessageRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.ConnectionDto
import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupDto
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto
import com.sleekydz86.kopanda.application.dto.response.MessageDto
import com.sleekydz86.kopanda.application.dto.response.PaginatedResponse
import com.sleekydz86.kopanda.application.dto.response.TopicDetailDto
import com.sleekydz86.kopanda.application.dto.response.TopicDto
=======
import com.sleekydz86.kopanda.application.dto.*
>>>>>>> origin/main
import com.sleekydz86.kopanda.application.ports.`in`.ActivityManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.ConnectionRepository
import com.sleekydz86.kopanda.application.ports.out.KafkaRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.domain.entities.Partition
import com.sleekydz86.kopanda.domain.entities.Message
<<<<<<< HEAD
import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
=======
import com.sleekydz86.kopanda.domain.valueobjects.ConnectionId
>>>>>>> origin/main
import com.sleekydz86.kopanda.shared.domain.DomainException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Service
@Transactional
class KafkaApplicationService(
    private val connectionRepository: ConnectionRepository,
    private val kafkaRepository: KafkaRepository,
    private val activityManagementUseCase: ActivityManagementUseCase
) : ConnectionManagementUseCase, KafkaManagementUseCase {

    private val logger = LoggerFactory.getLogger(KafkaApplicationService::class.java)

    override suspend fun getConnections(): List<ConnectionDto> {
        return connectionRepository.findAll().map { it.toConnectionDto() }
    }

    override suspend fun getConnection(id: String): ConnectionDto {
        val connection = getConnectionOrThrow(id)
        return connection.toConnectionDto()
    }

    override suspend fun createConnection(request: CreateConnectionRequest): ConnectionDto {
        val connection = Connection.create(
            name = request.name,
            host = request.host,
            port = request.port,
            sslEnabled = request.sslEnabled,
            saslEnabled = request.saslEnabled,
            username = request.username,
            password = request.password
        )

        val savedConnection = connectionRepository.save(connection)

        activityManagementUseCase.logConnectionCreated(
            connectionName = request.name,
            connectionId = savedConnection.getId().value
        )

        return savedConnection.toConnectionDto()
    }

    override suspend fun updateConnection(id: String, request: UpdateConnectionRequest): ConnectionDto {
        val connection = getConnectionOrThrow(id)

        connection.updateConnectionInfo(
            name = request.name,
            host = request.host,
            port = request.port,
            sslEnabled = request.sslEnabled,
            saslEnabled = request.saslEnabled,
            username = request.username,
            password = request.password
        )

        val updatedConnection = connectionRepository.save(connection)
        return updatedConnection.toConnectionDto()
    }

    override suspend fun deleteConnection(id: String) {
        val connection = getConnectionOrThrow(id)
        connection.delete()
        connectionRepository.delete(connection.getId())
    }

    override suspend fun testConnection(request: CreateConnectionRequest): ConnectionTestResult {
        val testConnection = Connection.create(
            name = request.name,
            host = request.host,
            port = request.port,
            sslEnabled = request.sslEnabled,
            saslEnabled = request.saslEnabled,
            username = request.username,
            password = request.password
        )

        return try {
            val startTime = System.currentTimeMillis()
            val isConnected = kafkaRepository.testConnection(testConnection)
            val latency = System.currentTimeMillis() - startTime

            if (isConnected) {
                val adminClient = createAdminClient(testConnection)
                val clusterDescription = adminClient.describeCluster().nodes().get()
                adminClient.close()

                ConnectionTestResult(
                    success = true,
                    message = "Connection successful",
                    latency = latency,
                    brokerInfo = BrokerInfo(
                        count = clusterDescription.size,
                        versions = listOf("3.9.0")
                    )
                )
            } else {
                ConnectionTestResult(
                    success = false,
                    message = "Connection failed"
                )
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                success = false,
                message = "Connection failed: ${e.message}"
            )
        }
    }

    override suspend fun getConnectionStatus(id: String): ConnectionStatus {
        val connection = getConnectionOrThrow(id)

        return try {
            val isConnected = kafkaRepository.testConnection(connection)
            val adminClient = createAdminClient(connection)
            val clusterDescription = adminClient.describeCluster().nodes().get()
            val topicList = adminClient.listTopics().names().get()
            adminClient.close()

            ConnectionStatus(
                connectionId = id,
                status = if (isConnected) ConnectionStatusType.CONNECTED else ConnectionStatusType.DISCONNECTED,
                lastChecked = LocalDateTime.now(),
                brokerCount = clusterDescription.size,
                topicCount = topicList.size
            )
        } catch (e: Exception) {
            activityManagementUseCase.logConnectionOffline(
                connectionName = connection.name.value,
                connectionId = connection.getId().value
            )

            ConnectionStatus(
                connectionId = id,
                status = ConnectionStatusType.ERROR,
                lastChecked = LocalDateTime.now(),
                errorMessage = e.message
            )
        }
    }

    override suspend fun refreshAllConnectionStatuses() {
        val startTime = LocalDateTime.now()
        val connections = connectionRepository.findAll()

        logger.info("Starting connection status refresh at $startTime for ${connections.size} connections")

        var successCount = 0
        var failureCount = 0

        connections.forEach { connection ->
            val connectionId = connection.getId().value
            val connectionName = connection.name.value

            try {
                logger.debug("Testing connection: $connectionName ($connectionId)")

                val isConnected = kafkaRepository.testConnection(connection)
                if (isConnected) {
                    connection.markAsConnected()
                    connectionRepository.save(connection)
                    successCount++
                    logger.debug("✓ Connection '$connectionName' ($connectionId) is healthy and status updated")
                } else {
                    failureCount++
                    logger.warn("✗ Connection '$connectionName' ($connectionId) is not responding")
                }

            } catch (e: Exception) {
                failureCount++
                logger.error(
                    "✗ Failed to test connection '$connectionName' ($connectionId): ${e.message}",
                    e
                )
            }
        }
        val endTime = LocalDateTime.now()
        val duration = java.time.Duration.between(startTime, endTime)

        logger.info(
            "Connection status refresh completed at $endTime " +
                    "(duration: ${duration.toMillis()}ms, " +
                    "successful: $successCount, " +
                    "failed: $failureCount, " +
                    "total: ${connections.size})"
        )
    }

    override suspend fun getTopics(connectionId: String): List<TopicDto> {
        val connection = getConnectionOrThrow(connectionId)
        val topics = kafkaRepository.getTopics(connection)
        return topics.map { it.toTopicDto() }
    }

    override suspend fun getTopicDetails(connectionId: String, topicName: String): TopicDetailDto {
        val connection = getConnectionOrThrow(connectionId)
<<<<<<< HEAD
        val topic = kafkaRepository.getTopicDetails(connection, TopicName(topicName))
=======
        val topic = kafkaRepository.getTopicDetails(connection, com.sleekydz86.kopanda.domain.valueobjects.TopicName(topicName))
>>>>>>> origin/main
            ?: throw DomainException("Topic '$topicName' not found")
        return topic.toTopicDetailDto()
    }

    override suspend fun createTopic(connectionId: String, request: CreateTopicRequest): TopicDto {
        val connection = getConnectionOrThrow(connectionId)
        val topic = Topic.create(
            name = request.name,
            partitionCount = request.partitions,
            replicationFactor = request.replicationFactor,
            config = request.config
        )
        val createdTopic = kafkaRepository.createTopic(connection, topic)
        return createdTopic.toTopicDto()
    }

    override suspend fun deleteTopic(connectionId: String, topicName: String) {
        val connection = getConnectionOrThrow(connectionId)
<<<<<<< HEAD
        kafkaRepository.deleteTopic(connection, TopicName(topicName))
=======
        kafkaRepository.deleteTopic(connection, com.sleekydz86.kopanda.domain.valueobjects.TopicName(topicName))
>>>>>>> origin/main
    }

    override suspend fun getMessages(
        connectionId: String,
        topicName: String,
        partitionNumber: Int,
        offset: Long?,
        offsetType: OffsetType,
        limit: Int
    ): PaginatedResponse<MessageDto> {
        val connection = getConnectionOrThrow(connectionId)
<<<<<<< HEAD
        val topic = kafkaRepository.getTopicDetails(connection, TopicName(topicName))
=======
        val topic = kafkaRepository.getTopicDetails(connection, com.sleekydz86.kopanda.domain.valueobjects.TopicName(topicName))
>>>>>>> origin/main
            ?: throw DomainException("Topic '$topicName' not found")

        val partition = topic.getPartition(partitionNumber)
            ?: throw DomainException("Partition $partitionNumber not found in topic '$topicName'")

        val actualOffset = when (offsetType) {
            OffsetType.EARLIEST -> partition.earliestOffset
            OffsetType.LATEST -> partition.latestOffset
            OffsetType.SPECIFIC -> offset ?: partition.earliestOffset
        }

        val messages = kafkaRepository.getMessages(
            connection,
            topic,
            partition,
<<<<<<< HEAD
            Offset(actualOffset),
=======
            com.sleekydz86.kopanda.domain.valueobjects.Offset(actualOffset),
>>>>>>> origin/main
            limit
        )

        return PaginatedResponse(
            items = messages.map { it.toMessageDto() },
            total = messages.size.toLong(),
            page = 1,
            pageSize = limit,
            totalPages = 1
        )
    }

    override suspend fun sendMessage(connectionId: String, topicName: String, request: SendMessageRequest) {
        val connection = getConnectionOrThrow(connectionId)
<<<<<<< HEAD
        val topic = kafkaRepository.getTopicDetails(connection, TopicName(topicName))
=======
        val topic = kafkaRepository.getTopicDetails(connection, com.sleekydz86.kopanda.domain.valueobjects.TopicName(topicName))
>>>>>>> origin/main
            ?: throw DomainException("Topic '$topicName' not found")

        kafkaRepository.sendMessage(
            connection,
            topic,
            request.key,
            request.value,
<<<<<<< HEAD
            request.partition?.let { PartitionNumber(it) },
=======
            request.partition?.let { com.sleekydz86.kopanda.domain.valueobjects.PartitionNumber(it) },
>>>>>>> origin/main
            request.headers
        )
    }

    override suspend fun searchMessages(connectionId: String, criteria: MessageSearchCriteria): List<MessageDto> {
        val connection = getConnectionOrThrow(connectionId)
        val messages = kafkaRepository.searchMessages(connection, criteria)
        return messages.map { it.toMessageDto() }
    }

    override suspend fun getConsumerGroups(connectionId: String): List<ConsumerGroupDto> {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getConsumerGroups(connection)
    }

    override suspend fun getMetrics(connectionId: String): KafkaMetricsDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getMetrics(connection)
    }

    private suspend fun getConnectionOrThrow(id: String): Connection {
        return connectionRepository.findById(ConnectionId(id))
            ?: throw DomainException("Connection '$id' not found")
    }

    private fun Connection.toConnectionDto(): ConnectionDto {
        return ConnectionDto(
            id = this.getId().value,
            name = this.name.value,
            host = this.host.value,
            port = this.port.value,
            sslEnabled = this.sslEnabled,
            saslEnabled = this.saslEnabled,
            username = this.username,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            lastConnected = this.lastConnected
        )
    }

    private fun Topic.toTopicDto(): TopicDto {
        return TopicDto(
            name = this.name.value,
            partitionCount = this.getPartitionCount(),
            replicationFactor = this.config.replicationFactor,
            messageCount = this.getTotalMessageCount(),
            isInternal = this.isInternal,
            isHealthy = this.isHealthy(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun Topic.toTopicDetailDto(): TopicDetailDto {
        return TopicDetailDto(
            name = this.name.value,
            partitionCount = this.getPartitionCount(),
            replicationFactor = this.config.replicationFactor,
            messageCount = this.getTotalMessageCount(),
            isInternal = this.isInternal,
            isHealthy = this.isHealthy(),
            config = this.config.config,
            partitions = this.partitions.map { it.toPartitionDto() },
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun Partition.toPartitionDto(): PartitionDto {
        return PartitionDto(
            partitionNumber = this.partitionNumber.value,
            leader = this.leader.value,
            replicas = this.replicas.map { it.value },
            inSyncReplicas = this.inSyncReplicas.map { it.value },
            earliestOffset = this.earliestOffset,
            latestOffset = this.latestOffset,
            messageCount = this.messageCount,
            isHealthy = this.isHealthy(),
            isUnderReplicated = this.isUnderReplicated()
        )
    }

    private fun Message.toMessageDto(): MessageDto {
        return MessageDto(
            offset = this.offset.value,
            key = this.key?.value,
            value = this.value.value,
            timestamp = this.timestamp.value,
            partition = this.partitionNumber.value,
            consumed = this.consumed,
            headers = this.headers,
            formattedTime = this.getFormattedTimestamp(),
            formattedOffset = this.getFormattedOffset()
        )
    }

    private fun createAdminClient(connection: Connection): org.apache.kafka.clients.admin.AdminClient {
        val props = java.util.Properties()
        props[org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = connection.getConnectionString()
        props[org.apache.kafka.clients.admin.AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG] = 5000
        props[org.apache.kafka.clients.admin.AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = 5000

        if (connection.sslEnabled) {
            props[org.apache.kafka.clients.admin.AdminClientConfig.SECURITY_PROTOCOL_CONFIG] = "SSL"
        }

        if (connection.saslEnabled && connection.username != null) {
            props[org.apache.kafka.clients.admin.AdminClientConfig.SECURITY_PROTOCOL_CONFIG] = "SASL_PLAINTEXT"
            props["sasl.mechanism"] = "PLAIN"
            props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${connection.username}\" password=\"${connection.password}\";"
        }

        return org.apache.kafka.clients.admin.AdminClient.create(props)
    }
}