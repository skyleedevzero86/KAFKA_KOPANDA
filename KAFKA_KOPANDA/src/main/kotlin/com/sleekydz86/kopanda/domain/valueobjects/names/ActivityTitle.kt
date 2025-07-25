package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class ActivityTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "Activity title cannot be blank" }
        require(value.length <= 200) { "Activity title cannot exceed 200 characters" }
    }

    override fun toString(): String = value
}