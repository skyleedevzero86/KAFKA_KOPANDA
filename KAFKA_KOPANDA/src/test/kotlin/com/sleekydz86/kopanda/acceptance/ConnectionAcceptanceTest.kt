package com.sleekydz86.kopanda.acceptance

import com.fasterxml.jackson.databind.ObjectMapper
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
        objectMapper = ObjectMapper()
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    fun `사용자는 Kafka 연결을 생성하고 관리할 수 있다`() {
        // given - 사용자가 새로운 Kafka 연결을 생성하려고 함
        val createRequest = CreateConnectionRequest(
            name = "production-kafka",
            host = "kafka.prod.com",
            port = 9092,
            sslEnabled = true,
            saslEnabled = true,
            username = "admin",
            password = "secure-password"
        )

        // when - 연결 생성 API 호출
        val createResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .`when`()
            .post("/connections")
            .then()
            .statusCode(201)
            .extract()
            .response()

        // then - 연결이 성공적으로 생성됨
        val createdConnection = objectMapper.readValue(createResponse.body.asString(), ConnectionDto::class.java)
        assertNotNull(createdConnection.id)
        assertEquals("production-kafka", createdConnection.name)
        assertEquals("kafka.prod.com", createdConnection.host)
        assertEquals(9092, createdConnection.port)
        assertTrue(createdConnection.sslEnabled)
        assertTrue(createdConnection.saslEnabled)

        // when - 생성된 연결 조회
        val getResponse = RestAssured.given()
            .`when`()
            .get("/connections/${createdConnection.id}")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 연결 정보가 올바르게 반환됨
        val retrievedConnection = objectMapper.readValue(getResponse.body.asString(), ConnectionDto::class.java)
        assertEquals(createdConnection.id, retrievedConnection.id)
        assertEquals(createdConnection.name, retrievedConnection.name)

        // when - 연결 정보 수정
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

        // then - 연결 정보가 성공적으로 수정됨
        val updatedConnection = objectMapper.readValue(updateResponse.body.asString(), ConnectionDto::class.java)
        assertEquals("updated-production-kafka", updatedConnection.name)
        assertEquals("kafka.prod.updated.com", updatedConnection.host)
        assertEquals(9093, updatedConnection.port)

        // when - 연결 목록 조회
        val listResponse = RestAssured.given()
            .`when`()
            .get("/connections")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 수정된 연결이 목록에 포함됨
        val connections = objectMapper.readValue(listResponse.body.asString(), Array<ConnectionDto>::class.java)
        assertTrue(connections.any { it.id == createdConnection.id })
        assertTrue(connections.any { it.name == "updated-production-kafka" })
    }

    @Test
    fun `사용자는 연결 상태를 모니터링할 수 있다`() {
        // given - 테스트용 연결 생성
        val connection = createTestConnection("monitoring-test")

        // when - 연결 상태 조회
        val statusResponse = RestAssured.given()
            .`when`()
            .get("/connections/${connection.id}/status")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 연결 상태 정보가 반환됨
        val status = objectMapper.readValue(statusResponse.body.asString(), Map::class.java)
        assertNotNull(status["connectionId"])
        assertNotNull(status["status"])
        assertNotNull(status["lastChecked"])

        // when - 연결 헬스 조회
        val healthResponse = RestAssured.given()
            .`when`()
            .get("/connections/${connection.id}/health")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 연결 헬스 정보가 반환됨
        val health = objectMapper.readValue(healthResponse.body.asString(), Map::class.java)
        assertNotNull(health["connectionId"])
        assertNotNull(health["isHealthy"])
        assertNotNull(health["healthScore"])

        // when - 연결 핑 테스트
        val pingResponse = RestAssured.given()
            .`when`()
            .get("/connections/${connection.id}/ping")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 핑 결과가 반환됨
        val ping = objectMapper.readValue(pingResponse.body.asString(), Map::class.java)
        assertNotNull(ping["connectionId"])
        assertNotNull(ping["isAlive"])
        assertNotNull(ping["responseTime"])
    }

    @Test
    fun `사용자는 연결을 테스트할 수 있다`() {
        // given - 테스트할 연결 정보
        val testRequest = CreateConnectionRequest(
            name = "test-connection",
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )

        // when - 연결 테스트 수행
        val testResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(testRequest)
            .`when`()
            .post("/connections/test")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 테스트 결과가 반환됨
        val testResult = objectMapper.readValue(testResponse.body.asString(), Map::class.java)
        assertNotNull(testResult["success"])
        assertNotNull(testResult["message"])
    }

    @Test
    fun `사용자는 연결 히스토리를 조회할 수 있다`() {
        // given - 테스트용 연결 생성
        val connection = createTestConnection("history-test")

        // when - 연결 히스토리 조회
        val historyResponse = RestAssured.given()
            .`when`()
            .get("/connections/${connection.id}/history")
            .param("limit", "10")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 연결 히스토리가 반환됨
        val history = objectMapper.readValue(historyResponse.body.asString(), Array<Map<String, Any>>::class.java)
        assertTrue(history.isNotEmpty())
        assertTrue(history.any { it["eventType"] == "CONNECTION_CREATED" })
    }

    @Test
    fun `사용자는 모든 연결 상태를 새로고침할 수 있다`() {
        // given - 테스트용 연결들 생성
        createTestConnection("refresh-test-1")
        createTestConnection("refresh-test-2")

        // when - 모든 연결 상태 새로고침
        val refreshResponse = RestAssured.given()
            .`when`()
            .post("/connections/refresh-status")
            .then()
            .statusCode(200)
            .extract()
            .response()

        // then - 새로고침이 성공적으로 완료됨
        assertEquals(200, refreshResponse.statusCode())
    }

    @Test
    fun `사용자는 잘못된 연결 정보로 생성할 수 없다`() {
        // given - 잘못된 연결 생성 요청 (빈 이름, 잘못된 포트)
        val invalidRequest = CreateConnectionRequest(
            name = "",
            host = "",
            port = 99999,
            sslEnabled = false,
            saslEnabled = false
        )

        // when & then - 400 Bad Request 반환
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
        // given - 첫 번째 연결 생성
        val firstConnection = createTestConnection("duplicate-test")

        // when - 동일한 이름으로 두 번째 연결 생성 시도
        val duplicateRequest = CreateConnectionRequest(
            name = "duplicate-test",
            host = "other-host",
            port = 9093
        )

        // then - 400 Bad Request 반환
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
        // given - 존재하지 않는 연결 ID
        val nonExistentId = "non-existent-connection-id"

        // when & then - 404 Not Found 반환
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

        return objectMapper.readValue(response.body.asString(), ConnectionDto::class.java)
    }
}
