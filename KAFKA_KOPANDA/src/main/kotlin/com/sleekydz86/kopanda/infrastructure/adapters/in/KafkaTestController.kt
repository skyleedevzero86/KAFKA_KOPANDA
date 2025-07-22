package com.sleekydz86.kopanda.infrastructure.adapters.`in`

import com.sleekydz86.kopanda.application.dto.*
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/test")
class KafkaTestController(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @PostMapping("/kafka-connection")
    suspend fun testKafkaConnection(): ResponseEntity<Map<String, Any?>> {
        val testRequest = CreateConnectionRequest(
            name = "Test Connection",
            host = "localhost",
            port = 29092,
            sslEnabled = false,
            saslEnabled = false
        )

        return try {
            val result = connectionManagementUseCase.testConnection(testRequest)
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
    suspend fun createTestConnection(): ResponseEntity<ConnectionDto> {
        val request = CreateConnectionRequest(
            name = "Local Kafka",
            host = "localhost",
            port = 29092,
            sslEnabled = false,
            saslEnabled = false
        )

        val connection = connectionManagementUseCase.createConnection(request)
        return ResponseEntity.ok(connection)
    }

    @GetMapping("/connection-status/{connectionId}")
    suspend fun getConnectionStatus(@PathVariable connectionId: String): ResponseEntity<ConnectionStatus> {
        val status = connectionManagementUseCase.getConnectionStatus(connectionId)
        return ResponseEntity.ok(status)
    }
}