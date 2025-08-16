package com.sleekydz86.kopanda.acceptance

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.ConnectionDto
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionAcceptanceTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("kafka_kopanda_test")
            withUsername("test")
            withPassword("test")
        }

        @Container
        val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).apply {
            withExposedPorts(9093)
            withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9093")
            withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9093")
            withEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:2181")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("kafka.bootstrap-servers", kafkaContainer::getBootstrapServers)
        }
    }

    @BeforeEach
    fun setUp() {
        objectMapper = jacksonObjectMapper()
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    fun `사용자는 Kafka 연결을 생성하고 관리할 수 있다`() {
        // given
        val createRequest = CreateConnectionRequest(
            name = "production-kafka",
            host = "kafka.prod.com",
            port = 9092,
            sslEnabled = true,
            saslEnabled = true,
            username = "admin",
            password = "secure-password"
        )

        // when
        val createResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .`when`()
            .post("/connections")
            .then()
            .statusCode(201)
            .extract()
            .response()

        // then
        val createdConnection = objectMapper.readValue<ConnectionDto>(createResponse.body.asString())
        assertNotNull(createdConnection.id)
        assertEquals("production-kafka", createdConnection.name)
        assertEquals("kafka.prod.com", createdConnection.host)
        assertEquals(9092, createdConnection.port)
        assertTrue(createdConnection.sslEnabled)
        assertTrue(createdConnection.saslEnabled)

        // when
        val getResponse = RestAssured.given()
            .`when`()
            .get("/connections/${createdConnection.id}")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val retrievedConnection = objectMapper.readValue<ConnectionDto>(getResponse.body.asString())
        assertEquals(createdConnection.id, retrievedConnection.id)
        assertEquals(createdConnection.name, retrievedConnection.name)

        // when
        val updateRequest = UpdateConnectionRequest(
            name = "updated-production-kafka",
            host = "kafka.prod.updated.com",
            port = 9093
        )

        val updateResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .`when`()
            .put("/connections/${createdConnection.id}")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val updatedConnection = objectMapper.readValue<ConnectionDto>(updateResponse.body.asString())
        assertEquals("updated-production-kafka", updatedConnection.name)
        assertEquals("kafka.prod.updated.com", updatedConnection.host)
        assertEquals(9093, updatedConnection.port)

        // when
        val listResponse = RestAssured.given()
            .`when`()
            .get("/connections")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val connections = objectMapper.readValue<Array<ConnectionDto>>(listResponse.body.asString())
        assertTrue(connections.any { it.id == createdConnection.id })
        assertTrue(connections.any { it.name == "updated-production-kafka" })
    }

    @Test
    fun `사용자는 연결 상태를 모니터링할 수 있다`() {
        // given
        val connection = createTestConnection("monitoring-test")

        // when
        val statusResponse = RestAssured.given()
            .`when`()
            .get("/connections/${connection.id}/status")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val status = objectMapper.readValue<Map<String, Any>>(statusResponse.body.asString())
        assertNotNull(status["connectionId"])
        assertNotNull(status["status"])
        assertNotNull(status["lastChecked"])

        // when
        val healthResponse = RestAssured.given()
            .`when`()
            .get("/connections/${connection.id}/health")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val health = objectMapper.readValue<Map<String, Any>>(healthResponse.body.asString())
        assertNotNull(health["connectionId"])
        assertNotNull(health["isHealthy"])
        assertNotNull(health["healthScore"])

        // when
        val pingResponse = RestAssured.given()
            .`when`()
            .get("/connections/${connection.id}/ping")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val ping = objectMapper.readValue<Map<String, Any>>(pingResponse.body.asString())
        assertNotNull(ping["connectionId"])
        assertNotNull(ping["isAlive"])
        assertNotNull(ping["responseTime"])
    }

    @Test
    fun `사용자는 연결을 테스트할 수 있다`() {
        // given
        val testRequest = CreateConnectionRequest(
            name = "test-connection",
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )

        // when
        val testResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(testRequest)
            .`when`()
            .post("/connections/test")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val testResult = objectMapper.readValue<Map<String, Any>>(testResponse.body.asString())
        assertNotNull(testResult["success"])
        assertNotNull(testResult["message"])
    }

    @Test
    fun `사용자는 연결 히스토리를 조회할 수 있다`() {
        // given
        val connection = createTestConnection("history-test")

        // when
        val historyResponse = RestAssured.given()
            .queryParam("limit", "10")
            .`when`()
            .get("/connections/${connection.id}/history")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then
        val history = objectMapper.readValue<List<Map<String, Any>>>(historyResponse.body.asString())
        assertTrue(history.isNotEmpty())
        assertTrue(history.any { it["eventType"] == "CONNECTION_CREATED" })
    }

    @Test
    fun `사용자는 모든 연결 상태를 새로고침할 수 있다`() {
        // given
        createTestConnection("refresh-test-1")
        createTestConnection("refresh-test-2")

        // when & then
        RestAssured.given()
            .`when`()
            .post("/connections/refresh-status")
            .then()
            .statusCode(200)
    }

    @Test
    fun `사용자는 잘못된 연결 정보로 생성할 수 없다`() {
        // given
        val invalidRequest = CreateConnectionRequest(
            name = "",
            host = "",
            port = 99999,
            sslEnabled = false,
            saslEnabled = false
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .`when`()
            .post("/connections")
            .then()
            .statusCode(400)
    }

    @Test
    fun `사용자는 중복된 연결 이름으로 생성할 수 없다`() {
        // given
        val firstConnection = createTestConnection("duplicate-test")

        // when
        val duplicateRequest = CreateConnectionRequest(
            name = "duplicate-test",
            host = "other-host",
            port = 9093
        )

        // then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(duplicateRequest)
            .`when`()
            .post("/connections")
            .then()
            .statusCode(400)
    }

    @Test
    fun `사용자는 존재하지 않는 연결을 조회할 수 없다`() {
        // given
        val nonExistentId = "non-existent-connection-id"

        // when & then
        RestAssured.given()
            .`when`()
            .get("/connections/$nonExistentId")
            .then()
            .statusCode(404)
    }

    private fun createTestConnection(name: String): ConnectionDto {
        val createRequest = CreateConnectionRequest(
            name = name,
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .`when`()
            .post("/connections")
            .then()
            .statusCode(201)
            .extract()
            .response()

        return objectMapper.readValue<ConnectionDto>(response.body.asString())
    }
}