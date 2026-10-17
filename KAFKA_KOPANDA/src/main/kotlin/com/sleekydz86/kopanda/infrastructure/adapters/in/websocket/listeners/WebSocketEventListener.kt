package com.sleekydz86.kopanda.infrastructure.adapters.`in`.websocket.listeners

import com.sleekydz86.kopanda.domain.events.ConnectionCreatedEvent
import com.sleekydz86.kopanda.domain.events.ConnectionDeletedEvent
import com.sleekydz86.kopanda.domain.events.ConnectionUpdatedEvent
import com.sleekydz86.kopanda.domain.events.TopicCreatedEvent
import com.sleekydz86.kopanda.domain.events.TopicDeletedEvent
import com.sleekydz86.kopanda.infrastructure.adapters.`in`.websocket.controllers.WebSocketController
import com.sleekydz86.kopanda.shared.domain.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class WebSocketEventListener(
    private val webSocketController: WebSocketController
) {

    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    suspend fun handleConnectionCreatedEvent(event: ConnectionCreatedEvent) {
        logger.info("연결 생성 이벤트 처리: ${event.connection.getId().value}")
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
        logger.info("연결 수정 이벤트 처리: ${event.connection.getId().value}")
        webSocketController.broadcastConnectionChange(event.connection.getId().value)
    }

    @EventListener
    suspend fun handleConnectionDeletedEvent(event: ConnectionDeletedEvent) {
        logger.info("연결 삭제 이벤트 처리: ${event.connection.getId().value}")
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
        logger.info("토픽 생성 이벤트 처리: ${event.topic.name.value}")
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
        logger.info("토픽 삭제 이벤트 처리: ${event.topic.name.value}")
        webSocketController.broadcastGlobalUpdate(
            "topic-deleted",
            mapOf(
                "topicName" to event.topic.name.value
            )
        )
    }

    @EventListener
    suspend fun handleGenericDomainEvent(event: DomainEvent) {
        logger.debug("일반 도메인 이벤트 처리: ${event::class.simpleName}")
        webSocketController.broadcastGlobalUpdate(
            "domain-event",
            mapOf(
                "eventType" to event::class.simpleName,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}