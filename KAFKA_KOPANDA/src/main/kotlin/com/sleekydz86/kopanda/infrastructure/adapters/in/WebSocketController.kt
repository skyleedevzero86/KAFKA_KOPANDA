package com.sleekydz86.kopanda.infrastructure.adapters.`in`

import com.sleekydz86.kopanda.application.dto.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.KafkaMetricsDto
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
//@RestController
//@RequestMapping("/api")
class WebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @MessageMapping("/subscribe/connection-status")
    @SendTo("/topic/connection-status")
    suspend fun subscribeToConnectionStatus(connectionId: String): com.sleekydz86.kopanda.infrastructure.adapters.`in`.WebSocketMessage<ConnectionStatus> {
        val status = connectionManagementUseCase.getConnectionStatus(connectionId)
        return com.sleekydz86.kopanda.infrastructure.adapters.`in`.WebSocketMessage(
            type = "connection-status",
            data = status,
            timestamp = LocalDateTime.now()
        )
    }

    @MessageMapping("/subscribe/metrics")
    @SendTo("/topic/metrics")
    suspend fun subscribeToMetrics(connectionId: String): com.sleekydz86.kopanda.infrastructure.adapters.`in`.WebSocketMessage<KafkaMetricsDto> {
        val metrics = kafkaManagementUseCase.getMetrics(connectionId)
        return com.sleekydz86.kopanda.infrastructure.adapters.`in`.WebSocketMessage(
            type = "metrics",
            data = metrics,
            timestamp = LocalDateTime.now()
        )
    }

    fun broadcastConnectionStatusUpdate(connectionId: String, status: ConnectionStatus) {
        val message = com.sleekydz86.kopanda.infrastructure.adapters.`in`.WebSocketMessage(
            type = "connection-status-update",
            data = status,
            timestamp = LocalDateTime.now()
        )
        messagingTemplate.convertAndSend("/topic/connection-status/$connectionId", message)
    }

    fun broadcastMetricsUpdate(connectionId: String, metrics: KafkaMetricsDto) {
        val message = com.sleekydz86.kopanda.infrastructure.adapters.`in`.WebSocketMessage(
            type = "metrics-update",
            data = metrics,
            timestamp = LocalDateTime.now()
        )
        messagingTemplate.convertAndSend("/topic/metrics/$connectionId", message)
    }
}