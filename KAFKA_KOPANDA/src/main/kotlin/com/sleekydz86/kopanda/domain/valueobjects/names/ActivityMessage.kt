package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class ActivityMessage(val value: String) {
    init {
        require(value.isNotBlank()) { "활동 메시지는 비어 있을 수 없습니다" }
        require(value.length <= 500) { "활동 메시지는 500자를 넘을 수 없습니다" }
    }

    override fun toString(): String = value
}