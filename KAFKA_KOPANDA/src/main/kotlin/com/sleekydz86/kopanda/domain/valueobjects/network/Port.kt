package com.sleekydz86.kopanda.domain.valueobjects.network

import jakarta.persistence.Embeddable

@Embeddable
data class Port(val value: Int) {
    init {
        require(value in 1..65535) { "포트는 1에서 65535 사이여야 합니다" }
    }

    fun isEqualTo(other: Port): Boolean = value == other.value

    fun isLessThan(other: Port): Boolean = value < other.value

    fun isGreaterThan(other: Port): Boolean = value > other.value

    override fun toString(): String = value.toString()
}