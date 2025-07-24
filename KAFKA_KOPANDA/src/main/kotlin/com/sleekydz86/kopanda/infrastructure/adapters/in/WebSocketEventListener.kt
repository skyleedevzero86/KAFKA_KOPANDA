package com.sleekydz86.kopanda.infrastructure.adapters.`in`

import com.sleekydz86.kopanda.domain.events.*
import com.sleekydz86.kopanda.shared.domain.DomainEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class WebSocketEventListener(
    private val webSocketController: WebSocketController
) {

    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    suspend fun handleConnectionCreatedEvent(event: ConnectionCreatedEvent) {
        logger.info("Handling ConnectionCreatedEvent for connection: ${event.connection.getId().value}")
        webSocketController.broadcastGlobalUpdate(
            "connection-created",
            mapOf(
                "connectionId" to event.connection.getId().value,
                "connectionName" to event.connection.name.value,
                "host" to event.connection.host.value,
                "port" to event.connection.port.value
            )
        )
    }

    @EventListener
    suspend fun handleConnectionUpdatedEvent(event: ConnectionUpdatedEvent) {
        logger.info("Handling ConnectionUpdatedEvent for connection: ${event.connection.getId().value}")
        webSocketController.broadcastConnectionChange(event.connection.getId().value)
    }

    @EventListener
    suspend fun handleConnectionDeletedEvent(event: ConnectionDeletedEvent) {
        logger.info("Handling ConnectionDeletedEvent for connection: ${event.connection.getId().value}")
        webSocketController.broadcastGlobalUpdate(
            "connection-deleted",
            mapOf(
                "connectionId" to event.connection.getId().value,
                "connectionName" to event.connection.name.value
            )
        )
    }

    @EventListener
    suspend fun handleTopicCreatedEvent(event: TopicCreatedEvent) {
        logger.info("Handling TopicCreatedEvent for topic: ${event.topic.name.value}")
        webSocketController.broadcastGlobalUpdate(
            "topic-created",
            mapOf(
                "topicName" to event.topic.name.value,
                "partitionCount" to event.topic.getPartitionCount(),
                "replicationFactor" to event.topic.config.replicationFactor
            )
        )
    }

    @EventListener
    suspend fun handleTopicDeletedEvent(event: TopicDeletedEvent) {
        logger.info("Handling TopicDeletedEvent for topic: ${event.topic.name.value}")
        webSocketController.broadcastGlobalUpdate(
            "topic-deleted",
            mapOf(
                "topicName" to event.topic.name.value
            )
        )
    }

    @EventListener
    suspend fun handleGenericDomainEvent(event: DomainEvent) {
        logger.debug("Handling generic domain event: ${event::class.simpleName}")
        // 기본 도메인 이벤트에 대한 처리
        webSocketController.broadcastGlobalUpdate(
            "domain-event",
            mapOf(
                "eventType" to event::class.simpleName,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}