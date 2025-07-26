package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import java.time.LocalDateTime

class ConnectionHistory(
    val connectionId: ConnectionId,
    val eventType: String,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val details: Map<String, Any> = emptyMap()
) {
    companion object {
        fun fromEvent(connectionId: ConnectionId, eventType: String, description: String, details: Map<String, Any> = emptyMap()): ConnectionHistory {
            return ConnectionHistory(
                connectionId = connectionId,
                eventType = eventType,
                description = description,
                timestamp = LocalDateTime.now(),
                details = details
            )
        }
    }
}