package com.sleekydz86.kopanda.application.services

import com.sleekydz86.kopanda.application.dto.common.*
import com.sleekydz86.kopanda.application.dto.enums.ConnectionStatusType
import com.sleekydz86.kopanda.application.dto.request.*
import com.sleekydz86.kopanda.application.dto.response.*
import com.sleekydz86.kopanda.application.ports.`in`.*
import com.sleekydz86.kopanda.application.ports.out.ConnectionHistoryRepository
import com.sleekydz86.kopanda.application.ports.out.ConnectionRepository
import com.sleekydz86.kopanda.application.ports.out.KafkaRepository
import com.sleekydz86.kopanda.domain.entities.*
import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import com.sleekydz86.kopanda.shared.domain.DomainException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.time.Duration
import java.util.Properties


@Service
@Transactional
class KafkaApplicationService(
    private val connectionRepository: ConnectionRepository,
    private val kafkaRepository: KafkaRepository,
    private val activityManagementUseCase: ActivityManagementUseCase,
    private val connectionHistoryRepository: ConnectionHistoryRepository
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
        logger.info("연결 생성 시작: ${request.name}")

        try {

            val existingConnection = connectionRepository.findByName(request.name)
            if (existingConnection != null) {
                logger.warn("중복된 연결 이름: ${request.name}")
                throw DomainException("이미 존재하는 연결 이름입니다: ${request.name}")
            }

            val connection = Connection.create(
                name = request.name,
                host = request.host,
                port = request.port,
                sslEnabled = request.sslEnabled,
                saslEnabled = request.saslEnabled,
                username = request.username,
                password = request.password
            )

            logger.info("연결 엔티티 생성 완료: ${connection.getId().value}")

            val savedConnection = connectionRepository.save(connection)
            logger.info("연결 저장 완료: ${savedConnection.getId().value}")

            activityManagementUseCase.logConnectionCreated(request.name, savedConnection.getId().value)

            val history = ConnectionHistory.create(
                connectionId = savedConnection.getId(),
                eventType = "CONNECTION_CREATED",
                description = "연결이 생성되었습니다: ${request.name}",
                details = mapOf(
                    "host" to request.host,
                    "port" to request.port.toString(),
                    "sslEnabled" to request.sslEnabled.toString(),
                    "saslEnabled" to request.saslEnabled.toString()
                )
            )
            connectionHistoryRepository.save(history)

            return savedConnection.toConnectionDto()
        } catch (e: Exception) {
            logger.error("연결 생성 실패: ${request.name}", e)
            throw e
        }
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

        val history = ConnectionHistory.create(
            connectionId = connection.getId(),
            eventType = "CONNECTION_UPDATED",
            description = "연결이 업데이트되었습니다: ${connection.name.value}",
            details = mapOf(
                "updatedFields" to listOfNotNull(
                    request.name?.let { "name" },
                    request.host?.let { "host" },
                    request.port?.let { "port" },
                    request.sslEnabled?.let { "sslEnabled" },
                    request.saslEnabled?.let { "saslEnabled" }
                )
            )
        )
        connectionHistoryRepository.save(history)

        return updatedConnection.toConnectionDto()
    }

    override suspend fun deleteConnection(id: String) {
        val connection = getConnectionOrThrow(id)
        connection.delete()
        connectionRepository.delete(connection.getId())

        val history = ConnectionHistory.create(
            connectionId = connection.getId(),
            eventType = "CONNECTION_DELETED",
            description = "연결이 삭제되었습니다: ${connection.name.value}",
            details = mapOf(
                "deletedAt" to LocalDateTime.now().toString()
            )
        )
        connectionHistoryRepository.save(history)
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
            logger.info("Checking connection status for: ${connection.name.value} (${connection.getConnectionString()})")

            val isConnected = kafkaRepository.testConnection(connection)

            if (isConnected) {

                connection.markAsConnected()
                connectionRepository.save(connection)

                val adminClient = createAdminClient(connection)
                val clusterDescription = adminClient.describeCluster().nodes().get()
                val topicList = adminClient.listTopics().names().get()
                adminClient.close()

                val history = ConnectionHistory.create(
                    connectionId = connection.getId(),
                    eventType = "CONNECTION_STATUS_CHECK",
                    description = "연결 상태 확인 성공",
                    details = mapOf(
                        "status" to "CONNECTED",
                        "brokerCount" to clusterDescription.size.toString(),
                        "topicCount" to topicList.size.toString()
                    )
                )
                connectionHistoryRepository.save(history)

                ConnectionStatus(
                    connectionId = id,
                    status = ConnectionStatusType.CONNECTED,
                    lastChecked = LocalDateTime.now(),
                    brokerCount = clusterDescription.size,
                    topicCount = topicList.size
                )
            } else {

                connection.markAsDisconnected()
                connectionRepository.save(connection)

                ConnectionStatus(
                    connectionId = id,
                    status = ConnectionStatusType.DISCONNECTED,
                    lastChecked = LocalDateTime.now(),
                    errorMessage = "Connection test failed"
                )
            }
        } catch (e: Exception) {
            logger.error("Connection status check failed for ${connection.name.value}: ${e.message}", e)

            connection.markAsError(e.message)
            connectionRepository.save(connection)

            val history = ConnectionHistory.create(
                connectionId = connection.getId(),
                eventType = "CONNECTION_STATUS_CHECK_FAILED",
                description = "연결 상태 확인 실패: ${e.message}",
                details = mapOf(
                    "status" to "ERROR",
                    "error" to (e.message ?: "Unknown error")

    val connection = getConnectionOrThrow(id)

    return try {
        logger.info("Checking connection status for: ${connection.name.value} (${connection.getConnectionString()})")
        
        val isConnected = kafkaRepository.testConnection(connection)
        
        if (isConnected) {

            connection.markAsConnected()
            connectionRepository.save(connection)
            
            val adminClient = createAdminClient(connection)
            val clusterDescription = adminClient.describeCluster().nodes().get()
            val topicList = adminClient.listTopics().names().get()
            adminClient.close()

            val history = ConnectionHistory.create(
                connectionId = connection.getId(),
                eventType = "CONNECTION_STATUS_CHECK",
                description = "연결 상태 확인 성공",
                details = mapOf(
                    "status" to "CONNECTED",
                    "brokerCount" to clusterDescription.size.toString(),
                    "topicCount" to topicList.size.toString()

                )
            )
            connectionHistoryRepository.save(history)


            activityManagementUseCase.logConnectionOffline(
                connectionName = connection.name.value,
                connectionId = connection.getId().value
            )

            ConnectionStatus(
                connectionId = id,
                status = ConnectionStatusType.CONNECTED,
                lastChecked = LocalDateTime.now(),
                brokerCount = clusterDescription.size,
                topicCount = topicList.size
            )
        } else {


            connection.markAsDisconnected()
            connectionRepository.save(connection)
            
            ConnectionStatus(
                connectionId = id,
                status = ConnectionStatusType.DISCONNECTED,
                lastChecked = LocalDateTime.now(),
                errorMessage = "Connection test failed"
            )
        }
    } catch (e: Exception) {
        logger.error("Connection status check failed for ${connection.name.value}: ${e.message}", e)

        connection.markAsError(e.message)
        connectionRepository.save(connection)

        val history = ConnectionHistory.create(
            connectionId = connection.getId(),
            eventType = "CONNECTION_STATUS_CHECK_FAILED",
            description = "연결 상태 확인 실패: ${e.message}",
            details = mapOf(
                "status" to "ERROR",
                "error" to (e.message ?: "Unknown error")
            )
        )
        connectionHistoryRepository.save(history)

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
                    connection.markAsDisconnected()
                    connectionRepository.save(connection)
                    failureCount++
                    logger.warn("✗ Connection '$connectionName' ($connectionId) is not responding")
                }

            } catch (e: Exception) {
                connection.markAsError(e.message)
                connectionRepository.save(connection)
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
        val topic = kafkaRepository.getTopicDetails(connection, TopicName(topicName))
            ?: throw DomainException("Topic '$topicName' not found")
        return topic.toTopicDetailDto()
    }

    override suspend fun createTopic(connectionId: String, request: CreateTopicRequest): TopicDto {
        logger.info("토픽 생성 시작: $connectionId/${request.name}")

        try {
            val connection = getConnectionOrThrow(connectionId)
            val topic = Topic.create(
                name = request.name,
                partitionCount = request.partitions,
                replicationFactor = request.replicationFactor,
                config = request.config
            )

            logger.info("토픽 엔티티 생성 완료: ${topic.name.value}")

            val createdTopic = kafkaRepository.createTopic(connection, topic)

            logger.info("토픽 생성 성공: ${createdTopic.name.value}")

            activityManagementUseCase.logTopicCreated(request.name)

            return createdTopic.toTopicDto()
        } catch (e: Exception) {
            logger.error("토픽 생성 실패: $connectionId/${request.name}", e)
            throw e
        }
    }

    override suspend fun deleteTopic(connectionId: String, topicName: String) {
        val connection = getConnectionOrThrow(connectionId)
        kafkaRepository.deleteTopic(connection, TopicName(topicName))
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
        val topic = kafkaRepository.getTopicDetails(connection, TopicName(topicName))
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
            Offset(actualOffset),
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
        val topic = kafkaRepository.getTopicDetails(connection, TopicName(topicName))
            ?: throw DomainException("Topic '$topicName' not found")

        kafkaRepository.sendMessage(
            connection,
            topic,
            request.key,
            request.value,
            request.partition?.let { PartitionNumber(it) },
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

    override suspend fun getTopicMetrics(connectionId: String): TopicMetricsDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getTopicMetrics(connection)
    }

    override suspend fun getDetailedMetrics(connectionId: String): DetailedMetricsDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getDetailedMetrics(connection)
    }

    override suspend fun getTopicHealth(connectionId: String, topicName: String): TopicHealthDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getTopicHealth(connection, TopicName(topicName))
    }

    override suspend fun getAllTopicsHealth(connectionId: String): List<TopicHealthDto> {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getAllTopicsHealth(connection)
    }

    override suspend fun getConsumerGroupMetrics(connectionId: String, groupId: String): ConsumerGroupMetricsDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getConsumerGroupMetrics(connection, groupId)
    }

    override suspend fun getAllConsumerGroupMetrics(connectionId: String): List<ConsumerGroupMetricsDto> {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getAllConsumerGroupMetrics(connection)
    }

    override suspend fun getPerformanceMetrics(connectionId: String): PerformanceMetricsDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getPerformanceMetrics(connection)
    }

    override suspend fun getPartitionDetails(connectionId: String, topicName: String, partitionNumber: Int): PartitionDetailDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getPartitionDetails(connection, TopicName(topicName), partitionNumber)
    }

    override suspend fun getOffsetInfo(connectionId: String, topicName: String, partitionNumber: Int): OffsetInfoDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getOffsetInfo(connection, TopicName(topicName), partitionNumber)
    }

    override suspend fun setOffset(connectionId: String, topicName: String, partitionNumber: Int, offset: Long): Boolean {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.setOffset(connection, TopicName(topicName), partitionNumber, offset)
    }

    override suspend fun getClusterInfo(connectionId: String): ClusterInfoDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.getClusterInfo(connection)
    }

    override suspend fun getConnectionHealth(id: String): ConnectionHealthDto {
        val connection = getConnectionOrThrow(id)
        val status = getConnectionStatus(id)

        val healthScore = when (status.status) {
            ConnectionStatusType.CONNECTED -> 100
            ConnectionStatusType.CONNECTING -> 50
            ConnectionStatusType.DISCONNECTED -> 25
            ConnectionStatusType.ERROR -> 0
        }

        val issues = mutableListOf<ConnectionIssueDto>()
        if (status.status == ConnectionStatusType.ERROR) {
            issues.add(ConnectionIssueDto(
                type = "CONNECTION_ERROR",
                severity = "HIGH",
                description = status.errorMessage ?: "Unknown error",
                timestamp = LocalDateTime.now()
            ))
        }

        return ConnectionHealthDto(
            connectionId = id,
            isHealthy = status.status == ConnectionStatusType.CONNECTED,
            healthScore = healthScore,
            lastCheckTime = status.lastChecked,
            responseTime = status.latency ?: 0,
            errorCount = if (status.status == ConnectionStatusType.ERROR) 1 else 0,
            successRate = if (status.status == ConnectionStatusType.CONNECTED) 100.0 else 0.0,
            issues = issues
        )
    }

    override suspend fun getAllConnectionsHealth(): List<ConnectionHealthDto> {
        val connections = connectionRepository.findAll()
        return connections.map { connection ->
            getConnectionHealth(connection.getId().value)
        }
    }

    override suspend fun getConnectionMetrics(id: String): ConnectionMetricsDto {
        val connection = getConnectionOrThrow(id)
        val uptime = java.time.Duration.between(connection.createdAt, LocalDateTime.now())

        return ConnectionMetricsDto(
            connectionId = id,
            uptime = uptime,
            totalRequests = 0L,
            successfulRequests = 0L,
            failedRequests = 0L,
            averageResponseTime = 0L,
            maxResponseTime = 0L,
            minResponseTime = 0L,
            lastActivity = connection.lastConnected ?: connection.updatedAt
        )
    }

    override suspend fun pingConnection(id: String): PingResultDto {
        val connection = getConnectionOrThrow(id)

        return try {
            val startTime = System.currentTimeMillis()
            val isAlive = kafkaRepository.testConnection(connection)
            val responseTime = System.currentTimeMillis() - startTime

            PingResultDto(
                connectionId = id,
                isAlive = isAlive,
                responseTime = responseTime,
                timestamp = LocalDateTime.now()
            )
        } catch (e: Exception) {
            PingResultDto(
                connectionId = id,
                isAlive = false,
                responseTime = 0,
                timestamp = LocalDateTime.now(),
                errorMessage = e.message
            )
        }
    }

    override suspend fun getConnectionHistory(id: String, limit: Int): List<ConnectionHistoryDto> {
        return connectionHistoryRepository.findByConnectionId(id, limit)
            .map {
                ConnectionHistoryDto(
                    connectionId = it.connectionId.value,
                    eventType = it.eventType,
                    description = it.description,
                    timestamp = it.timestamp,
                    details = it.details
                )
            }
    }

    override suspend fun createTestConsumerGroup(connectionId: String, topicName: String): ConsumerGroupDto {
        val connection = getConnectionOrThrow(connectionId)
        return kafkaRepository.createTestConsumerGroup(connection, topicName)
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