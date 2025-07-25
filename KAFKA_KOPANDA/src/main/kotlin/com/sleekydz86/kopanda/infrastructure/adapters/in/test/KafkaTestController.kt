package com.sleekydz86.kopanda.infrastructure.adapters.`in`.test

import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.CreateTopicRequest
import com.sleekydz86.kopanda.application.dto.response.ConnectionDto
import com.sleekydz86.kopanda.application.dto.response.TopicDto
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/test")
class KafkaTestController(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @PostMapping("/kafka-connection")
    fun testKafkaConnection(): ResponseEntity<Map<String, Any?>> {
        val testRequest = CreateConnectionRequest(
            name = "Test Connection",
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )

        return try {
            val result = runBlocking { connectionManagementUseCase.testConnection(testRequest) }
            ResponseEntity.ok(mapOf(
                "success" to result.success,
                "message" to result.message,
                "latency" to result.latency,
                "brokerInfo" to result.brokerInfo
            ))
        } catch (e: Exception) {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "message" to "Connection failed: ${e.message}",
                "error" to e.javaClass.simpleName
            ))
        }
    }

    @GetMapping("/kafka-connection")
    fun testKafkaConnectionGet(): ResponseEntity<Map<String, Any?>> {
        val testRequest = CreateConnectionRequest(
            name = "Test Connection",
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )

        return try {
            val result = runBlocking { connectionManagementUseCase.testConnection(testRequest) }
            ResponseEntity.ok(mapOf(
                "success" to result.success,
                "message" to result.message,
                "latency" to result.latency,
                "brokerInfo" to result.brokerInfo
            ))
        } catch (e: Exception) {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "message" to "Connection failed: ${e.message}",
                "error" to e.javaClass.simpleName
            ))
        }
    }

    @PostMapping("/create-test-connection")
    fun createTestConnection(): ResponseEntity<ConnectionDto> {
        val request = CreateConnectionRequest(
            name = "Local Kafka",
            host = "localhost",
            port = 9092,
            sslEnabled = false,
            saslEnabled = false
        )

        val connection = runBlocking { connectionManagementUseCase.createConnection(request) }
        return ResponseEntity.ok(connection)
    }

    @GetMapping("/connection-status/{connectionId}")
    fun getConnectionStatus(@PathVariable connectionId: String): ResponseEntity<ConnectionStatus> {
        val status = runBlocking { connectionManagementUseCase.getConnectionStatus(connectionId) }
        return ResponseEntity.ok(status)
    }

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any?>> {
        return ResponseEntity.ok(mapOf(
            "status" to "OK",
            "message" to "Kafka Test Controller is running",
            "timestamp" to LocalDateTime.now().toString()
        ))
    }

    @PostMapping("/create-test-topic/{connectionId}")
    fun createTestTopic(@PathVariable connectionId: String): ResponseEntity<TopicDto> {
        val request = CreateTopicRequest(
            name = "test-topic-${System.currentTimeMillis()}",
            partitions = 3,
            replicationFactor = 1,
            config = emptyMap()
        )

        val topic = runBlocking { kafkaManagementUseCase.createTopic(connectionId, request) }
        return ResponseEntity.ok(topic)
    }
}