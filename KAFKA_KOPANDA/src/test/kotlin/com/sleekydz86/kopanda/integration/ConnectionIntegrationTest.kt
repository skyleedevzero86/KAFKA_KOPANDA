package com.sleekydz86.kopanda.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.infrastructure.persistence.crypto.ConnectionPasswordCrypto
import com.sleekydz86.kopanda.infrastructure.persistence.crypto.PlainPasswordMigration
import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionEntity
import com.sleekydz86.kopanda.infrastructure.persistence.repositories.ConnectionJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class ConnectionIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var connectionJpaRepository: ConnectionJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var plainPasswordMigration: PlainPasswordMigration

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        objectMapper = ObjectMapper()

        connectionJpaRepository.deleteAll()
    }

    private fun MockMvc.performAsync(builder: RequestBuilder): ResultActions {
        val result = perform(builder).andExpect(request().asyncStarted()).andReturn()
        return perform(asyncDispatch(result))
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

        // when & then
        val createResponse = mockMvc.performAsync(
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
        val createdId = JsonPath.read<String>(responseBody, "$.id")

        // when & then
        mockMvc.performAsync(get("/connections/{connectionId}", createdId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdId))
            .andExpect(jsonPath("$.name").value("test-connection"))

        // when & then
        mockMvc.performAsync(get("/connections"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].name").value("test-connection"))
    }

    @Test
    fun `연결을 수정할 수 있다`() {
        // given
        val connection = createTestConnection("original-connection", "original-host", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        val updateRequest = UpdateConnectionRequest(
            name = "updated-connection",
            host = "updated-host",
            port = 9092
        )

        // when & then
        mockMvc.performAsync(
            put("/connections/{connectionId}", savedConnection.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("updated-connection"))
            .andExpect(jsonPath("$.host").value("updated-host"))
            .andExpect(jsonPath("$.port").value(9092))
    }

    @Test
    fun `연결을 삭제할 수 있다`() {
        // given
        val connection = createTestConnection("to-delete", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.performAsync(delete("/connections/{connectionId}", savedConnection.id))
            .andExpect(status().isNoContent)

        // when & then
        mockMvc.performAsync(get("/connections/{connectionId}", savedConnection.id))
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
        mockMvc.performAsync(
            post("/connections/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").exists())
    }

    @Test
    fun `연결 상태를 조회할 수 있다`() {
        // given
        val connection = createTestConnection("status-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.performAsync(get("/connections/{connectionId}/status", savedConnection.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(savedConnection.id))
            .andExpect(jsonPath("$.status").exists())
    }

    @Test
    fun `연결 헬스를 조회할 수 있다`() {
        // given
        val connection = createTestConnection("health-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.performAsync(get("/connections/{connectionId}/health", savedConnection.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(savedConnection.id))
            .andExpect(jsonPath("$.isHealthy").exists())
    }

    @Test
    fun `연결에 핑을 보낼 수 있다`() {
        // given
        val connection = createTestConnection("ping-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.performAsync(get("/connections/{connectionId}/ping", savedConnection.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionId").value(savedConnection.id))
            .andExpect(jsonPath("$.isAlive").exists())
    }

    @Test
    fun `연결 히스토리를 조회할 수 있다`() {
        // given
        val connection = createTestConnection("history-test", "localhost", 9092)
        val savedConnection = connectionJpaRepository.save(connection)

        // when & then
        mockMvc.performAsync(get("/connections/{connectionId}/history", savedConnection.id)
            .param("limit", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `모든 연결 상태를 새로고침할 수 있다`() {
        // given
        val connection = createTestConnection("refresh-test", "localhost", 9092)
        connectionJpaRepository.save(connection)

        // when & then
        mockMvc.performAsync(post("/connections/refresh-status"))
            .andExpect(status().isOk)
    }

    @Test
    fun `잘못된 연결 생성 요청 시 400을 반환한다`() {
        // given
        val invalidRequest = CreateConnectionRequest(
            name = "",
            host = "",
            port = 99999,
            sslEnabled = false,
            saslEnabled = false
        )

        // when & then
        mockMvc.performAsync(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `중복된 연결 이름으로 생성 시 400을 반환한다`() {
        // given
        val connection = createTestConnection("duplicate-name", "localhost", 9092)
        connectionJpaRepository.save(connection)

        // when & then
        val duplicateRequest = CreateConnectionRequest(
            name = "duplicate-name",
            host = "other-host",
            port = 9092
        )

        mockMvc.performAsync(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `저장된 비밀번호는 암호문이고 조회 응답에는 password 가 없다`() {
        // given
        val secret = "my-Secret-pw!"
        val createRequest = CreateConnectionRequest(
            name = "secret-connection",
            host = "localhost",
            port = 9092,
            password = secret
        )

        // when
        val createResponse = mockMvc.performAsync(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn()

        val body = createResponse.response.contentAsString
        val createdId = JsonPath.read<String>(body, "$.id")

        // then
        assertThat(body).doesNotContain(secret)
        entityManager.flush()
        entityManager.clear()
        mockMvc.performAsync(get("/connections/{connectionId}", createdId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.password").doesNotExist())
        mockMvc.performAsync(get("/connections"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].password").doesNotExist())

        val stored = jdbcTemplate.queryForObject(
            "SELECT password FROM connections WHERE connection_id = ?",
            String::class.java,
            createdId
        )
        assertThat(stored).startsWith(ConnectionPasswordCrypto.PREFIX)
        assertThat(stored).doesNotContain(secret)
    }

    @Test
    fun `평문으로 저장된 비밀번호는 암호화해서 다시 저장한다`() {
        // given
        val secret = "my-Secret-pw!"
        val now = Timestamp.valueOf(LocalDateTime.now())
        jdbcTemplate.update(
            """
            INSERT INTO connections (
                connection_id, connection_name, host, port, ssl_enabled, sasl_enabled,
                username, password, created_at, updated_at, last_connected, is_deleted
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            "legacy-plain",
            "legacy-plain",
            "localhost",
            9092,
            false,
            false,
            null,
            secret,
            now,
            now,
            null,
            false
        )

        // when
        val changed = plainPasswordMigration.encryptStoredPlaintext()

        // then
        assertThat(changed).isEqualTo(1)
        entityManager.clear()
        val stored = jdbcTemplate.queryForObject(
            "SELECT password FROM connections WHERE connection_id = ?",
            String::class.java,
            "legacy-plain"
        )
        assertThat(stored).startsWith(ConnectionPasswordCrypto.PREFIX)
        assertThat(stored).doesNotContain(secret)
        assertThat(connectionJpaRepository.findById("legacy-plain").orElseThrow().password).isEqualTo(secret)
        assertThat(plainPasswordMigration.encryptStoredPlaintext()).isEqualTo(0)
    }

    @Test
    fun `비밀번호가 128자를 넘으면 400을 반환한다`() {
        // given
        val createRequest = CreateConnectionRequest(
            name = "too-long-password",
            host = "localhost",
            port = 9092,
            password = "a".repeat(129)
        )

        // when & then
        mockMvc.performAsync(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isBadRequest)

        val saved = connectionJpaRepository.save(createTestConnection("keep-short", "localhost", 9092))
        val updateRequest = UpdateConnectionRequest(password = "b".repeat(129))
        mockMvc.performAsync(
            put("/connections/{connectionId}", saved.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `수정 요청에 password 가 없으면 기존 비밀번호를 유지한다`() {
        // given
        val secret = "my-Secret-pw!"
        val createResponse = mockMvc.performAsync(
            post("/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateConnectionRequest(
                            name = "keep-password",
                            host = "localhost",
                            port = 9092,
                            password = secret
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
        val createdId = JsonPath.read<String>(createResponse.response.contentAsString, "$.id")

        // when
        mockMvc.performAsync(
            put("/connections/{connectionId}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateConnectionRequest(name = "renamed")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("renamed"))
            .andExpect(jsonPath("$.password").doesNotExist())

        entityManager.flush()
        entityManager.clear()
        val reloaded = connectionJpaRepository.findById(createdId).orElseThrow()
        assertThat(reloaded.name).isEqualTo("renamed")
        assertThat(reloaded.password).isEqualTo(secret)
        val stored = jdbcTemplate.queryForObject(
            "SELECT password FROM connections WHERE connection_id = ?",
            String::class.java,
            createdId
        )
        assertThat(stored).startsWith(ConnectionPasswordCrypto.PREFIX)
        assertThat(stored).doesNotContain(secret)
    }

    private fun createTestConnection(name: String, host: String, port: Int): ConnectionEntity {
        return ConnectionEntity(
            id = UUID.randomUUID().toString(),
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
            isDeleted = false
        )
    }
}