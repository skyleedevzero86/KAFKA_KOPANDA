package com.sleekydz86.kopanda.domain.valueobjects.message

import jakarta.persistence.Embeddable
import jakarta.persistence.Column

@Embeddable
data class MessageValue(
    @Column(columnDefinition = "TEXT")
    val value: String
) {
    init {
        require(value.isNotBlank()) { "Message value cannot be blank" }
    }

    fun isNull(): Boolean = false

    fun getBytes(): ByteArray = value.toByteArray()

    fun length(): Int = value.length

    fun truncate(maxLength: Int): MessageValue {
        return if (value.length <= maxLength) {
            this
        } else {
            MessageValue(value.substring(0, maxLength) + "...")
        }
    }

    override fun toString(): String = value
}