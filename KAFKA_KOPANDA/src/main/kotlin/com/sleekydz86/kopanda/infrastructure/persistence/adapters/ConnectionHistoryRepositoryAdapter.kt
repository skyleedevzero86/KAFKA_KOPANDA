package com.sleekydz86.kopanda.infrastructure.persistence.adapters

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.kopanda.domain.entities.ConnectionHistory
import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionHistoryEntity
import com.sleekydz86.kopanda.infrastructure.persistence.repositories.ConnectionHistoryJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ConnectionHistoryRepositoryAdapter(
    private val connectionHistoryJpaRepository: ConnectionHistoryJpaRepository,
    private val objectMapper: ObjectMapper
) {

    suspend fun save(history: ConnectionHistory): ConnectionHistory {
        val entity = ConnectionHistoryEntity(
            id = history.id,
            connectionId = history.connectionId.value,
            eventType = history.eventType,
            description = history.description,
            timestamp = history.timestamp,
            details = objectMapper.writeValueAsString(history.details)
        )
        
        val savedEntity = connectionHistoryJpaRepository.save(entity)
        
        return ConnectionHistory(
            id = savedEntity.id,
            connectionId = ConnectionId(savedEntity.connectionId),
            eventType = savedEntity.eventType,
            description = savedEntity.description,
            timestamp = savedEntity.timestamp,
            details = objectMapper.readValue(savedEntity.details, object : TypeReference<Map<String, Any>>() {})
        )
    }

    suspend fun findByConnectionId(connectionId: String, limit: Int): List<ConnectionHistory> {
        val entities = connectionHistoryJpaRepository.findByConnectionIdOrderByTimestampDesc(connectionId, limit)
        
        return entities.map { entity ->
            ConnectionHistory(
                id = entity.id,
                connectionId = ConnectionId(entity.connectionId),
                eventType = entity.eventType,
                description = entity.description,
                timestamp = entity.timestamp,
                details = objectMapper.readValue(entity.details, object : TypeReference<Map<String, Any>>() {})
            )
        }
    }
}