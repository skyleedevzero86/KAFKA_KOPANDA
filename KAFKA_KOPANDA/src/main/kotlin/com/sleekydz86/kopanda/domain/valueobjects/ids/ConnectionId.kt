package com.sleekydz86.kopanda.domain.valueobjects.ids

import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
data class ConnectionId(val value: String) {
    init {
        require(value.isNotBlank()) { "연결 ID는 비어 있을 수 없습니다" }
    }

    companion object {
        fun generate(): ConnectionId = ConnectionId(UUID.randomUUID().toString())
    }

    override fun toString(): String = value
}