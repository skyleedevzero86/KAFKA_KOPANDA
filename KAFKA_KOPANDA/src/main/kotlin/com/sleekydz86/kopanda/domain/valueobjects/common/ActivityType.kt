package com.sleekydz86.kopanda.domain.valueobjects.common

import jakarta.persistence.Embeddable

@Embeddable
data class ActivityType(val value: String) {
    init {
        require(value.isNotBlank()) { "활동 유형은 비어 있을 수 없습니다" }
        require(value in listOf("CONNECTION_CREATED", "CONNECTION_UPDATED", "CONNECTION_DELETED",
            "CONNECTION_OFFLINE", "TOPIC_CREATED", "TOPIC_DELETED",
            "MESSAGE_SENT", "ERROR_OCCURRED")) {
            "올바르지 않은 활동 유형입니다: $value"
        }
    }

    override fun toString(): String = value
}