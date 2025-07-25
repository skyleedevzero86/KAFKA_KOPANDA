package com.sleekydz86.kopanda.domain.valueobjects

import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
data class ActivityId(val value: String) {
    init {
        require(value.isNotBlank()) { "Activity ID cannot be blank" }
    }

    companion object {
        fun generate(): ActivityId = ActivityId(UUID.randomUUID().toString())
    }

    override fun toString(): String = value
}