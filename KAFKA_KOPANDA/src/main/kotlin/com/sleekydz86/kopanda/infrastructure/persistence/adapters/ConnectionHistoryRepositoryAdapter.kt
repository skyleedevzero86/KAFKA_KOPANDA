package com.sleekydz86.kopanda.infrastructure.persistence.adapters

import com.sleekydz86.kopanda.application.ports.out.ConnectionHistoryRepository
import com.sleekydz86.kopanda.domain.entities.ConnectionHistory
import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionHistoryEntity
import com.sleekydz86.kopanda.infrastructure.persistence.repositories.ConnectionHistoryJpaRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository

@Repository
@Primary
class ConnectionHistoryRepositoryAdapter(
    private val jpaRepository: ConnectionHistoryJpaRepository
) : ConnectionHistoryRepository {
    override suspend fun save(history: ConnectionHistory) {
        val entity = ConnectionHistoryEntity(
            connectionId = history.connectionId.value,
            eventType = history.eventType,
            description = history.description,
            timestamp = history.timestamp,
            details = history.details.mapValues { it.value.toString() }
        )
        jpaRepository.save(entity)
    }

    override suspend fun findByConnectionId(connectionId: String, limit: Int): List<ConnectionHistory> {
        return jpaRepository.findByConnectionId(connectionId)
            .take(limit)
            .map {
                ConnectionHistory(
                    connectionId = ConnectionId(it.connectionId),
                    eventType = it.eventType,
                    description = it.description,
                    timestamp = it.timestamp,
                    details = it.details
                )
            }
    }
}