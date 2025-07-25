package com.sleekydz86.kopanda.domain.valueobjects.message

import jakarta.persistence.Embeddable

@Embeddable
data class Offset(val value: Long) {
    init {
        require(value >= 0) { "Offset must be non-negative" }
    }

    fun increment(): Offset = Offset(value + 1)

    fun decrement(): Offset = Offset(maxOf(0, value - 1))

    fun add(delta: Long): Offset = Offset(maxOf(0, value + delta))

    fun subtract(delta: Long): Offset = Offset(maxOf(0, value - delta))

    fun isGreaterThan(other: Offset): Boolean = value > other.value

    fun isLessThan(other: Offset): Boolean = value < other.value

    fun isEqualTo(other: Offset): Boolean = value == other.value

    override fun toString(): String = value.toString()
}