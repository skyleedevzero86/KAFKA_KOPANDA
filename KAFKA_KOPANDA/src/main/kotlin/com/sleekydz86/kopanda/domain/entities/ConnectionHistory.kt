package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import com.sleekydz86.kopanda.shared.domain.AggregateRoot
import java.time.LocalDateTime

data class ConnectionHistory(
    val id: String,
    val connectionId: ConnectionId,
    val eventType: String,
    val description: String,
    val timestamp: LocalDateTime,
    val details: Map<String, Any> = emptyMap()
) : AggregateRoot() {

    companion object {
        fun create(
            connectionId: ConnectionId,
            eventType: String,
            description: String,
            details: Map<String, Any> = emptyMap()
        ): ConnectionHistory {
            return ConnectionHistory(
                id = java.util.UUID.randomUUID().toString(),
                connectionId = connectionId,
                eventType = eventType,
                description = description,
                timestamp = LocalDateTime.now(),
                details = details
            )
        }
    }
}