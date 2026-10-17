package com.sleekydz86.kopanda.domain.valueobjects.message

import jakarta.persistence.Embeddable

@Embeddable
data class MessageKey(val value: String) {
    init {
        require(value.isNotBlank()) { "메시지 키는 비어 있을 수 없습니다" }
    }

    fun isNull(): Boolean = false

    fun getBytes(): ByteArray = value.toByteArray()

    fun length(): Int = value.length

    override fun toString(): String = value
}