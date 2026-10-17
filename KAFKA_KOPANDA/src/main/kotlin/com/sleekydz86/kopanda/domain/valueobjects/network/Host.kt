package com.sleekydz86.kopanda.domain.valueobjects.network

import jakarta.persistence.Embeddable

@Embeddable
data class Host(val value: String) {
    init {
        require(value.isNotBlank()) { "호스트는 비어 있을 수 없습니다" }
        require(value.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
            "호스트는 영문, 숫자, 점, 하이픈만 사용할 수 있습니다"
        }
    }

    fun isValid(): Boolean = isHostName(value) || isIpv4(value)

    override fun toString(): String = value

    private fun isHostName(host: String): Boolean {
        if (host.length !in 1..253) return false
        return host.split('.').all { label ->
            label.length in 1..63 && label.matches(Regex("^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?$"))
        }
    }

    private fun isIpv4(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.matches(Regex("^(0|[1-9]\\d{0,2})$")) && part.toInt() in 0..255
        }
    }
}