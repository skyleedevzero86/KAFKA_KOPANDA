package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.common.ConnectionTestResult
import com.sleekydz86.kopanda.application.dto.enums.ConnectionStatusType
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.*
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import com.sleekydz86.kopanda.shared.domain.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import com.sleekydz86.kopanda.application.dto.common.BrokerInfo

class ConnectionControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var connectionManagementUseCase: ConnectionManagementUseCase
    private lateinit var kafkaManagementUseCase: KafkaManagementUseCase
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        connectionManagementUseCase = mockk()
        kafkaManagementUseCase = mockk()
        objectMapper = ObjectMapper()
        
        val controller = ConnectionController(connectionManagementUseCase, kafkaManagementUseCase)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `연결 목록을 조회할 수 있다`() = runTest {
        // given
        val connections = listOf(
            createTestConnectionDto("conn-1", "test-connection-1"),
            createTestConnectionDto("conn-2", "test-connection-2")
        )
        coEvery { connectionManagementUseCase.getConnections() } returns connections

        // when & then
        mockMvc.perform(get("/connections"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").value("conn-1"))
            .andExpect(jsonPath("$[1].id").value("conn-2"))

        coVerify { connectionManagementUseCase.getConnections() }
    }

    @Test
    fun `특정 연결을 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val connection = createTestConnectionDto(connectionId, "test-connection")
        coEvery { connectionManagementUseCase.getConnection(connectionId) } returns connection

        // when & then
        mockMvc.perform(get("/connections/{connectionId}", connectionId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(connectionId))
            .andExpect(jsonPath("$.name").value("test-connection"))

        coVerify { connectionManagementUseCase.getConnection(connectionId) }
    }

    @Test
    fun `존재하지 않는 연결을 조회하면 404를 반환한다`() = runTest {
        // given
        val connectionId = "non-existent"
        coEvery { connectionManagementUseCase.getConnection(connectionId) } throws DomainException("Connection not found")

        // when & then
        mockMvc.perform(get("/connections/{connectionId}", connectionId))
            .andExpect(status().isNotFound)

        coVerify { connectionManagementUseCase.getConnection(connectionId) }
    }

    @Test
    fun `새로운 연결을 생성할 수 있다`() = runTest {
        // given
        val createRequest = CreateConnectionRequest(
            name = "new-connection",
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )
        val createdConnection = createTestConnectionDto("conn-new", "new-connection")
        coEvery { connectionManagementUseCase.createConnection(any()) } returns createdConnection

        // when & then
        mockMvc.perform(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("conn-new"))
            .andExpect(jsonPath("$.name").value("new-connection"))

        coVerify { connectionManagementUseCase.createConnection(any()) }
    }

    @Test
    fun `연결을 수정할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val updateRequest = UpdateConnectionRequest(
            name = "updated-connection",
            host = "new-host",
            port = 9093
        )
        val updatedConnection = createTestConnectionDto(connectionId, "updated-connection")
        coEvery { connectionManagementUseCase.updateConnection(connectionId, any()) } returns updatedConnection

        // when & then
        mockMvc.perform(
            put("/connections/{connectionId}", connectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("updated-connection"))

        coVerify { connectionManagementUseCase.updateConnection(connectionId, any()) }
    }

    @Test
    fun `연결을 삭제할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        coEvery { connectionManagementUseCase.deleteConnection(connectionId) } returns Unit

        // when & then
        mockMvc.perform(delete("/connections/{connectionId}", connectionId))
            .andExpect(status().isNoContent)

        coVerify { connectionManagementUseCase.deleteConnection(connectionId) }
    }

    @Test
    fun `연결을 테스트할 수 있다`() = runTest {
        // given
        val testRequest = CreateConnectionRequest(
            name = "test-connection",
            host = "localhost",
            port = 9092
        )
        val testResult = ConnectionTestResult(
            success = true,
            message = "Connection successful",
            latency = 50L,
            brokerInfo = BrokerInfo(3, listOf("3.9.0"))
        )
        coEvery { connectionManagementUseCase.testConnection(any()) } returns testResult

        // when & then
        mockMvc.perform(
            post("/connections/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.latency").value(50))

        coVerify { connectionManagementUseCase.testConnection(any()) }
    }

    @Test
    fun `연결 상태를 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val status = ConnectionStatus(
            connectionId = connectionId,
            status = ConnectionStatusType.CONNECTED,
            lastChecked = LocalDateTime.now(),
            brokerCount = 3,
            topicCount = 10
        )
        coEvery { connectionManagementUseCase.getConnectionStatus(connectionId) } returns status

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/status", connectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(connectionId))
            .andExpect(jsonPath("$.status").value("CONNECTED"))

        coVerify { connectionManagementUseCase.getConnectionStatus(connectionId) }
    }

    @Test
    fun `토픽 목록을 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val topics = listOf(
            createTestTopicDto("topic-1"),
            createTestTopicDto("topic-2")
        )
        coEvery { kafkaManagementUseCase.getTopics(connectionId) } returns topics

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/topics", connectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].name").value("topic-1"))
            .andExpect(jsonPath("$[1].name").value("topic-2"))

        coVerify { kafkaManagementUseCase.getTopics(connectionId) }
    }

    @Test
    fun `컨슈머 그룹 목록을 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val consumerGroups = listOf(
            createTestConsumerGroupDto("group-1"),
            createTestConsumerGroupDto("group-2")
        )
        coEvery { kafkaManagementUseCase.getConsumerGroups(connectionId) } returns consumerGroups

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/consumer-groups", connectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].groupId").value("group-1"))
            .andExpect(jsonPath("$[1].groupId").value("group-2"))

        coVerify { kafkaManagementUseCase.getConsumerGroups(connectionId) }
    }

    @Test
    fun `연결 메트릭을 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val metrics = createTestKafkaMetricsDto()
        coEvery { kafkaManagementUseCase.getMetrics(connectionId) } returns metrics

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/metrics", connectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.brokerCount").value(3))
            .andExpect(jsonPath("$.topicCount").value(10))

        coVerify { kafkaManagementUseCase.getMetrics(connectionId) }
    }

    @Test
    fun `연결 헬스를 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val health = createTestConnectionHealthDto(connectionId)
        coEvery { connectionManagementUseCase.getConnectionHealth(connectionId) } returns health

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/health", connectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(connectionId))
            .andExpect(jsonPath("$.isHealthy").value(true))

        coVerify { connectionManagementUseCase.getConnectionHealth(connectionId) }
    }

    @Test
    fun `연결에 핑을 보낼 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val pingResult = createTestPingResultDto(connectionId)
        coEvery { connectionManagementUseCase.pingConnection(connectionId) } returns pingResult

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/ping", connectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(connectionId))
            .andExpect(jsonPath("$.isAlive").value(true))

        coVerify { connectionManagementUseCase.pingConnection(connectionId) }
    }

    @Test
    fun `연결 히스토리를 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val history = listOf(
            createTestConnectionHistoryDto(connectionId, "CONNECTION_CREATED"),
            createTestConnectionHistoryDto(connectionId, "CONNECTION_UPDATED")
        )
        coEvery { connectionManagementUseCase.getConnectionHistory(connectionId, 10) } returns history

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/history", connectionId)
            .param("limit", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].eventType").value("CONNECTION_CREATED"))
            .andExpect(jsonPath("$[1].eventType").value("CONNECTION_UPDATED"))

        coVerify { connectionManagementUseCase.getConnectionHistory(connectionId, 10) }
    }

    @Test
    fun `모든 연결 상태를 새로고침할 수 있다`() = runTest {
        // given
        coEvery { connectionManagementUseCase.refreshAllConnectionStatuses() } returns Unit

        // when & then
        mockMvc.perform(post("/connections/refresh-status"))
            .andExpect(status().isOk)

        coVerify { connectionManagementUseCase.refreshAllConnectionStatuses() }
    }

    private fun createTestConnectionDto(id: String, name: String): ConnectionDto {
        return ConnectionDto(
            id = id,
            name = name,
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false,
            username = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            lastConnected = null
        )
    }

    private fun createTestTopicDto(name: String): TopicDto {
        return TopicDto(
            name = name,
            partitionCount = 3,
            replicationFactor = 2,
            messageCount = 1000L,
            isInternal = false,
            isHealthy = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createTestConsumerGroupDto(groupId: String): ConsumerGroupDto {
        return ConsumerGroupDto(
            groupId = groupId,
            state = "Stable",
            memberCount = 2,
            topicCount = 1,
            offsets = mapOf("topic-1" to 100L)
        )
    }

    private fun createTestKafkaMetricsDto(): KafkaMetricsDto {
        return KafkaMetricsDto(
            brokerCount = 3,
            topicCount = 10,
            totalPartitions = 30,
            messagesPerSecond = 100.0,
            bytesInPerSec = 1024.0,
            bytesOutPerSec = 512.0,
            activeConnections = 5,
            timestamp = LocalDateTime.now()
        )
    }

    private fun createTestConnectionHealthDto(connectionId: String): ConnectionHealthDto {
        return ConnectionHealthDto(
            connectionId = connectionId,
            isHealthy = true,
            healthScore = 100,
            lastCheckTime = LocalDateTime.now(),
            responseTime = 50L,
            errorCount = 0,
            successRate = 100.0,
            issues = emptyList()
        )
    }

    private fun createTestPingResultDto(connectionId: String): PingResultDto {
        return PingResultDto(
            connectionId = connectionId,
            isAlive = true,
            responseTime = 50L,
            timestamp = LocalDateTime.now()
        )
    }

    private fun createTestConnectionHistoryDto(connectionId: String, eventType: String): ConnectionHistoryDto {
        return ConnectionHistoryDto(
            connectionId = connectionId,
            eventType = eventType,
            description = "Test event",
            timestamp = LocalDateTime.now()
        )
    }
}
