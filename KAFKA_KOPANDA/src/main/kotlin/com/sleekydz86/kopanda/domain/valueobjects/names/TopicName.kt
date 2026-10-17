package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class TopicName(val value: String) {
    init {
        require(value.isNotBlank()) { "토픽 이름은 비어 있을 수 없습니다" }
        require(value != "." && value != "..") { "토픽 이름은 \".\" 또는 \"..\"일 수 없습니다" }
        require(value.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            "토픽 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다"
        }
        require(value.length <= 249) { "토픽 이름은 249자를 넘을 수 없습니다" }
    }

    val isInternal: Boolean
        get() = value.startsWith("_") || value.startsWith(".")

    override fun toString(): String = value
}