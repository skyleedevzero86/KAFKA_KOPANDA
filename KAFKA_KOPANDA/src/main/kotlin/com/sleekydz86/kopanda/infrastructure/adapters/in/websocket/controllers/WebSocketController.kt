package com.sleekydz86.kopanda.infrastructure.adapters.`in`.websocket.controllers

import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import com.sleekydz86.kopanda.infrastructure.adapters.`in`.websocket.dto.WebSocketMessage
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class WebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @MessageMapping("/subscribe/connection-status")
    @SendTo("/topic/connection-status")
    suspend fun subscribeToConnectionStatus(connectionId: String): WebSocketMessage<ConnectionStatus> {
        val status = connectionManagementUseCase.getConnectionStatus(connectionId)
        return WebSocketMessage(
            type = "connection-status",
            data = status,
            timestamp = LocalDateTime.now()
        )
    }

    @MessageMapping("/subscribe/metrics")
    @SendTo("/topic/metrics")
    suspend fun subscribeToMetrics(connectionId: String): WebSocketMessage<KafkaMetricsDto> {
        val metrics = kafkaManagementUseCase.getMetrics(connectionId)
        return WebSocketMessage(
            type = "metrics",
            data = metrics,
            timestamp = LocalDateTime.now()
        )
    }

    fun broadcastConnectionStatusUpdate(connectionId: String, status: ConnectionStatus) {
        val message = WebSocketMessage(
            type = "connection-status-update",
            data = status,
            timestamp = LocalDateTime.now()
        )
        messagingTemplate.convertAndSend("/topic/connection-status/$connectionId", message)
    }

    fun broadcastMetricsUpdate(connectionId: String, metrics: KafkaMetricsDto) {
        val message = WebSocketMessage(
            type = "metrics-update",
            data = metrics,
            timestamp = LocalDateTime.now()
        )
        messagingTemplate.convertAndSend("/topic/metrics/$connectionId", message)
    }

    fun broadcastGlobalUpdate(type: String, data: Any) {
        val message = WebSocketMessage(
            type = type,
            data = data,
            timestamp = LocalDateTime.now()
        )
        messagingTemplate.convertAndSend("/topic/global", message)
    }

    fun broadcastConnectionChange(connectionId: String) {
        val message = WebSocketMessage(
            type = "connection-change",
            data = mapOf("connectionId" to connectionId),
            timestamp = LocalDateTime.now()
        )
        messagingTemplate.convertAndSend("/topic/connection-change", message)
    }

    fun broadcastAllConnectionsUpdate() {
        val message = WebSocketMessage(
            type = "all-connections-update",
            data = mapOf("timestamp" to LocalDateTime.now()),
            timestamp = LocalDateTime.now()
        )
        messagingTemplate.convertAndSend("/topic/all-connections", message)
    }
}