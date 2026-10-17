package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class TopicName(val value: String) {
    init {
        require(value.isNotBlank()) { "Topic name cannot be blank" }
        require(value != "." && value != "..") { "Topic name cannot be \".\" or \"..\"" }
        require(value.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            "Topic name can only contain alphanumeric characters, dots, underscores, and hyphens"
        }
        require(value.length <= 249) { "Topic name cannot exceed 249 characters" }
    }

    val isInternal: Boolean
        get() = value.startsWith("_") || value.startsWith(".")

    override fun toString(): String = value
}