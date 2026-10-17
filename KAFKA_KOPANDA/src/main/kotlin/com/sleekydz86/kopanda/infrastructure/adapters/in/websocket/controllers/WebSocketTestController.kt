package com.sleekydz86.kopanda.infrastructure.adapters.`in`.websocket.controllers

import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.enums.ConnectionStatusType
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/websocket-test")
class WebSocketTestController(
    private val webSocketController: WebSocketController
) {

    @PostMapping("/broadcast/connection-status/{connectionId}")
    fun broadcastConnectionStatus(@PathVariable connectionId: String): ResponseEntity<Map<String, Any>> {
        val status = ConnectionStatus(
            connectionId = connectionId,
            status = ConnectionStatusType.CONNECTED,
            lastChecked = LocalDateTime.now(),
            brokerCount = 3,
            topicCount = 10,
            latency = 50L
        )

        runBlocking {
            webSocketController.broadcastConnectionStatusUpdate(connectionId, status)
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "연결 상태를 전송했습니다",
            "connectionId" to connectionId,
            "timestamp" to LocalDateTime.now()
        ))
    }

    @PostMapping("/broadcast/metrics/{connectionId}")
    fun broadcastMetrics(@PathVariable connectionId: String): ResponseEntity<Map<String, Any>> {
        val metrics = KafkaMetricsDto(
            brokerCount = 3,
            topicCount = 15,
            totalPartitions = 45,
            messagesPerSecond = 1000.0,
            bytesInPerSec = 1024000.0,
            bytesOutPerSec = 512000.0,
            activeConnections = 25,
            timestamp = LocalDateTime.now()
        )

        runBlocking {
            webSocketController.broadcastMetricsUpdate(connectionId, metrics)
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "메트릭을 전송했습니다",
            "connectionId" to connectionId,
            "timestamp" to LocalDateTime.now()
        ))
    }

    @PostMapping("/broadcast/global")
    fun broadcastGlobalMessage(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val type = request["type"] as? String ?: "test-message"
        val data = request["data"] ?: "Test data"

        runBlocking {
            webSocketController.broadcastGlobalUpdate(type, data)
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "전체 메시지를 전송했습니다",
            "type" to type,
            "timestamp" to LocalDateTime.now()
        ))
    }

    @PostMapping("/simulate/connection-change/{connectionId}")
    fun simulateConnectionChange(@PathVariable connectionId: String): ResponseEntity<Map<String, Any>> {
        runBlocking {
            webSocketController.broadcastConnectionChange(connectionId)
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "연결 변경을 재현했습니다",
            "connectionId" to connectionId,
            "timestamp" to LocalDateTime.now()
        ))
    }

    @GetMapping("/status")
    fun getWebSocketStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "active",
            "message" to "웹소켓 컨트롤러가 동작 중입니다",
            "endpoints" to listOf(
                "/topic/connection-status/{connectionId}",
                "/topic/metrics/{connectionId}",
                "/topic/all-connections",
                "/topic/global",
                "/topic/connection-change"
            ),
            "timestamp" to LocalDateTime.now()
        ))
    }
}