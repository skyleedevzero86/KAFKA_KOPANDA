package com.sleekydz86.kopanda.domain.events

import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.shared.domain.DomainEvent

data class ConnectionUpdatedEvent(val connection: Connection) : DomainEvent