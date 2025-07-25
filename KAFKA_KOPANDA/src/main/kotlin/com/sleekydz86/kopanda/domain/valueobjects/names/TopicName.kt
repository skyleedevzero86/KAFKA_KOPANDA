package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class TopicName(val value: String) {
    init {
        require(value.isNotBlank()) { "Topic name cannot be blank" }
        require(value.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            "Topic name can only contain alphanumeric characters, dots, underscores, and hyphens"
        }
        require(value.length <= 249) { "Topic name cannot exceed 249 characters" }
        require(!value.startsWith(".") && !value.startsWith("_")) {
            "Topic name cannot start with '.' or '_'"
        }
    }

    val isInternal: Boolean
        get() = value.startsWith("__")

    override fun toString(): String = value
}