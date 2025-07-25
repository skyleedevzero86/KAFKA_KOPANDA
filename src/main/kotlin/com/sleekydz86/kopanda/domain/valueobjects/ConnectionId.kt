package com.sleekydz86.kopanda.domain.valueobjects

import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
data class ConnectionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Connection ID cannot be blank" }
    }

    companion object {
        fun generate(): ConnectionId = ConnectionId(UUID.randomUUID().toString())
    }

    override fun toString(): String = value
}