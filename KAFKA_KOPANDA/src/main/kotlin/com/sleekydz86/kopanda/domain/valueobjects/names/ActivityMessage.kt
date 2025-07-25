package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class ActivityMessage(val value: String) {
    init {
        require(value.isNotBlank()) { "Activity message cannot be blank" }
        require(value.length <= 500) { "Activity message cannot exceed 500 characters" }
    }

    override fun toString(): String = value
}