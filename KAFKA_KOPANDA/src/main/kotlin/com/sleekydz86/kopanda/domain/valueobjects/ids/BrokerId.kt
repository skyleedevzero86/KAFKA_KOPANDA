package com.sleekydz86.kopanda.domain.valueobjects.ids

import jakarta.persistence.Embeddable

@Embeddable
data class BrokerId(val value: Int) {
    init {
        require(value >= 0) { "브로커 ID는 0 이상이어야 합니다" }
    }

    fun isEqualTo(other: BrokerId): Boolean = value == other.value

    fun isLessThan(other: BrokerId): Boolean = value < other.value

    fun isGreaterThan(other: BrokerId): Boolean = value > other.value

    override fun toString(): String = value.toString()
}