package com.sleekydz86.kopanda.domain.valueobjects.topic

import jakarta.persistence.Embeddable

@Embeddable
data class PartitionNumber(val value: Int) {
    init {
        require(value >= 0) { "Partition number must be non-negative" }
    }

    fun increment(): PartitionNumber = PartitionNumber(value + 1)

    fun decrement(): PartitionNumber = PartitionNumber(maxOf(0, value - 1))

    fun isEqualTo(other: PartitionNumber): Boolean = value == other.value

    fun isLessThan(other: PartitionNumber): Boolean = value < other.value

    fun isGreaterThan(other: PartitionNumber): Boolean = value > other.value

    override fun toString(): String = value.toString()
}