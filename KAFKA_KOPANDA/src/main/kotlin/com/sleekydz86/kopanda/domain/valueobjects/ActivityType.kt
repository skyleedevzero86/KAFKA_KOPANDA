package com.sleekydz86.kopanda.domain.valueobjects

import jakarta.persistence.Embeddable

@Embeddable
data class ActivityType(val value: String) {
    init {
        require(value.isNotBlank()) { "Activity type cannot be blank" }
        require(value in listOf("CONNECTION_CREATED", "CONNECTION_UPDATED", "CONNECTION_DELETED",
            "CONNECTION_OFFLINE", "TOPIC_CREATED", "TOPIC_DELETED",
            "MESSAGE_SENT", "ERROR_OCCURRED")) {
            "Invalid activity type: $value"
        }
    }

    override fun toString(): String = value
}