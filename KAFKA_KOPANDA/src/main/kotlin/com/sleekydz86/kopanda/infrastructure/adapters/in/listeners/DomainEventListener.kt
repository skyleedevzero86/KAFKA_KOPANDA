package com.sleekydz86.kopanda.infrastructure.adapters.`in`.listeners

import com.sleekydz86.kopanda.application.ports.out.ConnectionHistoryRepository
import com.sleekydz86.kopanda.domain.entities.ConnectionHistory
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.events.*
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class DomainEventListener(
    private val connectionHistoryRepository: ConnectionHistoryRepository
) {

    private val logger = LoggerFactory.getLogger(DomainEventListener::class.java)

    @EventListener
    suspend fun handleConnectionCreated(event: ConnectionCreatedEvent) {
        try {
            connectionHistoryRepository.save(
                ConnectionHistory.fromEvent(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_CREATED",
                    description = "Connection created: ${event.connection.name.value}"
                )
            )
            logger.info("Connection history saved for created connection: ${event.connection.name.value}")
        } catch (e: Exception) {
            logger.error("Failed to save connection history for created connection: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleConnectionUpdated(event: ConnectionUpdatedEvent) {
        try {
            connectionHistoryRepository.save(
                ConnectionHistory.fromEvent(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_UPDATED",
                    description = "Connection updated: ${event.connection.name.value}"
                )
            )
            logger.info("Connection history saved for updated connection: ${event.connection.name.value}")
        } catch (e: Exception) {
            logger.error("Failed to save connection history for updated connection: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleConnectionDeleted(event: ConnectionDeletedEvent) {
        try {
            connectionHistoryRepository.save(
                ConnectionHistory.fromEvent(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_DELETED",
                    description = "Connection deleted: ${event.connection.name.value}"
                )
            )
            logger.info("Connection history saved for deleted connection: ${event.connection.name.value}")
        } catch (e: Exception) {
            logger.error("Failed to save connection history for deleted connection: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleConnectionStatusChanged(event: ConnectionStatusChangedEvent) {
        try {
            val statusDescription = when (event.newStatus) {
                Connection.ConnectionStatus.CONNECTED -> "Connection established"
                Connection.ConnectionStatus.DISCONNECTED -> "Connection lost"
                Connection.ConnectionStatus.CONNECTING -> "Connection attempt"
                Connection.ConnectionStatus.ERROR -> "Connection error: ${event.errorMessage ?: "Unknown error"}"
            }

            connectionHistoryRepository.save(
                ConnectionHistory.fromEvent(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_STATUS_CHANGED",
                    description = "Status changed to ${event.newStatus}: ${event.connection.name.value} - $statusDescription",
                    details = mapOf(
                        "previousStatus" to event.previousStatus.name,
                        "newStatus" to event.newStatus.name,
                        "errorMessage" to (event.errorMessage ?: "")
                    )
                )
            )
            logger.info("Connection status history saved: ${event.connection.name.value} ${event.previousStatus} -> ${event.newStatus}")
        } catch (e: Exception) {
            logger.error("Failed to save connection status history: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleTopicCreated(event: TopicCreatedEvent) {
        try {
            logger.info("Topic created: ${event.topic.name.value}")
        } catch (e: Exception) {
            logger.error("Failed to handle topic created event: ${event.topic.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleTopicDeleted(event: TopicDeletedEvent) {
        try {
            logger.info("Topic deleted: ${event.topic.name.value}")
        } catch (e: Exception) {
            logger.error("Failed to handle topic deleted event: ${event.topic.name.value}", e)
        }
    }
}