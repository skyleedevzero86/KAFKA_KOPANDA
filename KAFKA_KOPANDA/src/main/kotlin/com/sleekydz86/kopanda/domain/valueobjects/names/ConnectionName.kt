package com.sleekydz86.kopanda.domain.valueobjects.names

import jakarta.persistence.Embeddable

@Embeddable
data class ConnectionName(val value: String) {
    init {
        require(value.isNotBlank()) { "연결 이름은 비어 있을 수 없습니다" }
        require(value.length <= 100) { "연결 이름은 100자를 넘을 수 없습니다" }
        require(value.matches(Regex("^[a-zA-Z0-9가-힣\\s_-]+$"))) {
            "연결 이름은 영문, 숫자, 한글, 공백, 밑줄, 하이픈만 사용할 수 있습니다"
        }
    }

    override fun toString(): String = value
}