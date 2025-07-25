package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class ConnectionName(val value: String) {
    init {
        require(value.isNotBlank()) { "Connection name cannot be blank" }
        require(value.length <= 100) { "Connection name cannot exceed 100 characters" }
        require(value.matches(Regex("^[a-zA-Z0-9가-힣\\s_-]+$"))) {
            "Connection name can only contain alphanumeric characters, Korean characters, spaces, underscores, and hyphens"
        }
    }

    override fun toString(): String = value
}