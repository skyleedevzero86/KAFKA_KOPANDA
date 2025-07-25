package com.sleekydz86.kopanda.domain.valueobjects

import jakarta.persistence.Embeddable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Embeddable
data class Timestamp(val value: Long) {
    init {
        require(value >= 0) { "Timestamp must be non-negative" }
    }

    fun toInstant(): Instant = Instant.ofEpochMilli(value)

    fun toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault())

    fun isAfter(other: Timestamp): Boolean = value > other.value

    fun isBefore(other: Timestamp): Boolean = value < other.value

    fun isEqualTo(other: Timestamp): Boolean = value == other.value

    fun addMillis(millis: Long): Timestamp = Timestamp(value + millis)

    fun subtractMillis(millis: Long): Timestamp = Timestamp(maxOf(0, value - millis))

    override fun toString(): String = value.toString()
}