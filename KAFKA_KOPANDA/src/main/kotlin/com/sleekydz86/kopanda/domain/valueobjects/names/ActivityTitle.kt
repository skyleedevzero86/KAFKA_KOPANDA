package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class ActivityTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "활동 제목은 비어 있을 수 없습니다" }
        require(value.length <= 200) { "활동 제목은 200자를 넘을 수 없습니다" }
    }

    override fun toString(): String = value
}