package com.sleekydz86.kopanda.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.ConnectionDto
import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionEntity
import com.sleekydz86.kopanda.infrastructure.persistence.repositories.ConnectionJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class ConnectionIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var connectionJpaRepository: ConnectionJpaRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        objectMapper = ObjectMapper()
        
        // 테스트 데이터 정리
        connectionJpaRepository.deleteAll()
    }

    @Test
    fun `연결을 생성하고 조회할 수 있다`() {
        // given
        val createRequest = CreateConnectionRequest(
            name = "test-connection",
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )

        // when & then - 연결 생성
        val createResponse = mockMvc.perform(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("test-connection"))
            .andExpect(jsonPath("$.host").value("localhost"))
            .andExpect(jsonPath("$.port").value(9092))
            .andReturn()

        val responseBody = createResponse.response.contentAsString
        val createdConnection = objectMapper.readValue(responseBody, ConnectionDto::class.java)

        // when & then - 생성된 연결 조회
        mockMvc.perform(get("/connections/{connectionId}", createdConnection.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdConnection.id))
            .andExpect(jsonPath("$.name").value("test-connection"))

        // when & then - 연결 목록 조회
        mockMvc.perform(get("/connections"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].name").value("test-connection"))
    }

    @Test
    fun `연결을 수정할 수 있다`() {
        // given - 연결 생성
        val connection = createTestConnection("original-connection", "original-host", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        val updateRequest = UpdateConnectionRequest(
            name = "updated-connection",
            host = "updated-host",
            port = 9093
        )

        // when & then - 연결 수정
        mockMvc.perform(
            put("/connections/{connectionId}", savedConnection.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("updated-connection"))
            .andExpect(jsonPath("$.host").value("updated-host"))
            .andExpect(jsonPath("$.port").value(9093))
    }

    @Test
    fun `연결을 삭제할 수 있다`() {
        // given - 연결 생성
        val connection = createTestConnection("to-delete", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then - 연결 삭제
        mockMvc.perform(delete("/connections/{connectionId}", savedConnection.id))
            .andExpect(status().isNoContent)

        // when & then - 삭제된 연결 조회 시 404 반환
        mockMvc.perform(get("/connections/{connectionId}", savedConnection.id))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `연결 테스트를 수행할 수 있다`() {
        // given
        val testRequest = CreateConnectionRequest(
            name = "test-connection",
            host = "localhost",
            port = 9092
        )

        // when & then
        mockMvc.perform(
            post("/connections/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").exists())
    }

    @Test
    fun `연결 상태를 조회할 수 있다`() {
        // given - 연결 생성
        val connection = createTestConnection("status-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/status", savedConnection.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(savedConnection.id))
            .andExpect(jsonPath("$.status").exists())
    }

    @Test
    fun `연결 헬스를 조회할 수 있다`() {
        // given - 연결 생성
        val connection = createTestConnection("health-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/health", savedConnection.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(savedConnection.id))
            .andExpect(jsonPath("$.isHealthy").exists())
    }

    @Test
    fun `연결에 핑을 보낼 수 있다`() {
        // given - 연결 생성
        val connection = createTestConnection("ping-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/ping", savedConnection.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(savedConnection.id))
            .andExpect(jsonPath("$.isAlive").exists())
    }

    @Test
    fun `연결 히스토리를 조회할 수 있다`() {
        // given - 연결 생성
        val connection = createTestConnection("history-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.perform(get("/connections/{connectionId}/history", savedConnection.id)
            .param("limit", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `모든 연결 상태를 새로고침할 수 있다`() {
        // given - 연결 생성
        val connection = createTestConnection("refresh-test", "localhost", 9092)
        connectionJpaRepository.save(connection)

        // when & then
        mockMvc.perform(post("/connections/refresh-status"))
            .andExpect(status().isOk)
    }

    @Test
    fun `잘못된 연결 생성 요청 시 400을 반환한다`() {
        // given - 잘못된 요청 (포트 범위 초과)
        val invalidRequest = CreateConnectionRequest(
            name = "",
            host = "",
            port = 99999,
            sslEnabled = false,
            saslEnabled = false
        )

        // when & then
        mockMvc.perform(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `중복된 연결 이름으로 생성 시 400을 반환한다`() {
        // given - 첫 번째 연결 생성
        val connection = createTestConnection("duplicate-name", "localhost", 9092)
        connectionJpaRepository.save(connection)

        // when & then - 동일한 이름으로 두 번째 연결 생성 시도
        val duplicateRequest = CreateConnectionRequest(
            name = "duplicate-name",
            host = "other-host",
            port = 9093
        )

        mockMvc.perform(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest))
        )
            .andExpect(status().isBadRequest)
    }

    private fun createTestConnection(name: String, host: String, port: Int): ConnectionEntity {
        return ConnectionEntity(
            id = null,
            name = name,
            host = host,
            port = port,
            sslEnabled = false,
            saslEnabled = false,
            username = null,
            password = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            lastConnected = null,
            isDeleted = false,
            status = "DISCONNECTED"
        )
    }
}
