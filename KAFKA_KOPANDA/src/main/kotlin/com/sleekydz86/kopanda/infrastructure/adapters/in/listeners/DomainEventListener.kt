package com.sleekydz86.kopanda.infrastructure.adapters.`in`.listeners

import com.sleekydz86.kopanda.application.ports.out.ConnectionHistoryRepository
import com.sleekydz86.kopanda.domain.entities.ConnectionHistory
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.events.*
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Component
class DomainEventListener(
    private val connectionHistoryRepository: ConnectionHistoryRepository
) {

    private val logger = LoggerFactory.getLogger(DomainEventListener::class.java)

    @EventListener
    suspend fun handleConnectionCreated(event: ConnectionCreatedEvent) {
        try {
            connectionHistoryRepository.save(
                ConnectionHistory.create(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_CREATED",
                    description = "연결이 생성되었습니다: ${event.connection.name.value}",
                    details = mapOf(
                        "connectionName" to event.connection.name.value,
                        "host" to event.connection.host.value,
                        "port" to event.connection.port.value.toString()
                    )
                )
            )
            logger.info("생성된 연결의 이력을 저장했습니다: ${event.connection.name.value}")
        } catch (e: Exception) {
            logger.error("생성된 연결의 이력 저장에 실패했습니다: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleConnectionUpdated(event: ConnectionUpdatedEvent) {
        try {
            connectionHistoryRepository.save(
                ConnectionHistory.create(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_UPDATED",
                    description = "연결이 수정되었습니다: ${event.connection.name.value}",
                    details = mapOf(
                        "connectionName" to event.connection.name.value,
                        "updatedAt" to event.connection.updatedAt.toString()
                    )
                )
            )
            logger.info("수정된 연결의 이력을 저장했습니다: ${event.connection.name.value}")
        } catch (e: Exception) {
            logger.error("수정된 연결의 이력 저장에 실패했습니다: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleConnectionDeleted(event: ConnectionDeletedEvent) {
        try {
            connectionHistoryRepository.save(
                ConnectionHistory.create(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_DELETED",
                    description = "연결이 삭제되었습니다: ${event.connection.name.value}",
                    details = mapOf(
                        "connectionName" to event.connection.name.value,
                        "deletedAt" to LocalDateTime.now().toString()
                    )
                )
            )
            logger.info("삭제된 연결의 이력을 저장했습니다: ${event.connection.name.value}")
        } catch (e: Exception) {
            logger.error("삭제된 연결의 이력 저장에 실패했습니다: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleConnectionStatusChanged(event: ConnectionStatusChangedEvent) {
        try {
            val statusDescription = when (event.newStatus) {
                Connection.ConnectionStatus.CONNECTED -> "연결되었습니다"
                Connection.ConnectionStatus.DISCONNECTED -> "연결이 끊어졌습니다"
                Connection.ConnectionStatus.CONNECTING -> "연결을 시도합니다"
                Connection.ConnectionStatus.ERROR -> "연결 오류: ${event.errorMessage ?: "알 수 없는 오류"}"
            }

            connectionHistoryRepository.save(
                ConnectionHistory.create(
                    connectionId = event.connection.getId(),
                    eventType = "CONNECTION_STATUS_CHANGED",
                    description = "상태가 ${event.newStatus}로 변경되었습니다: ${event.connection.name.value} - $statusDescription",
                    details = mapOf(
                        "previousStatus" to event.previousStatus.name,
                        "newStatus" to event.newStatus.name,
                        "errorMessage" to (event.errorMessage ?: ""),
                        "connectionName" to event.connection.name.value
                    )
                )
            )
            logger.info("연결 상태 이력을 저장했습니다: ${event.connection.name.value} ${event.previousStatus} -> ${event.newStatus}")
        } catch (e: Exception) {
            logger.error("연결 상태 이력 저장에 실패했습니다: ${event.connection.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleTopicCreated(event: TopicCreatedEvent) {
        try {
            logger.info("토픽이 생성되었습니다: ${event.topic.name.value}")
        } catch (e: Exception) {
            logger.error("토픽 생성 이벤트 처리에 실패했습니다: ${event.topic.name.value}", e)
        }
    }

    @EventListener
    suspend fun handleTopicDeleted(event: TopicDeletedEvent) {
        try {
            logger.info("토픽이 삭제되었습니다: ${event.topic.name.value}")
        } catch (e: Exception) {
            logger.error("토픽 삭제 이벤트 처리에 실패했습니다: ${event.topic.name.value}", e)
        }
    }
}