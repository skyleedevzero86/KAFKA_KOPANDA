package com.sleekydz86.kopanda.domain.events

import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.shared.domain.DomainEvent

data class ConnectionStatusChangedEvent(
    val connection: Connection,
    val previousStatus: Connection.ConnectionStatus,
    val newStatus: Connection.ConnectionStatus,
    val errorMessage: String? = null
) : DomainEvent