package com.sleekydz86.kopanda.domain.valueobjects

import jakarta.persistence.Embeddable

@Embeddable
data class MessageKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Message key cannot be blank" }
    }

    fun isNull(): Boolean = false

    fun getBytes(): ByteArray = value.toByteArray()

    fun length(): Int = value.length

    override fun toString(): String = value
}