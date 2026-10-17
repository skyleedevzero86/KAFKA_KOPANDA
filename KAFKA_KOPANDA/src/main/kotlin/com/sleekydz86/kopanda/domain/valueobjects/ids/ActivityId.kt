package com.sleekydz86.kopanda.domain.valueobjects.ids

import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
data class ActivityId(val value: String) {
    init {
        require(value.isNotBlank()) { "활동 ID는 비어 있을 수 없습니다" }
    }

    companion object {
        fun generate(): ActivityId = ActivityId(UUID.randomUUID().toString())
    }

    override fun toString(): String = value
}