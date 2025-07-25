package com.sleekydz86.kopanda.domain.valueobjects

import jakarta.persistence.Embeddable
import java.net.InetAddress

@Embeddable
data class Host(val value: String) {
    init {
        require(value.isNotBlank()) { "Host cannot be blank" }
        require(value.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
            "Host can only contain alphanumeric characters, dots, and hyphens"
        }
    }

    fun isValid(): Boolean {
        return try {
            InetAddress.getByName(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun toString(): String = value
}