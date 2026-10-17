package com.sleekydz86.kopanda.domain.valueobjects.message

import jakarta.persistence.Embeddable

@Embeddable
data class OffsetRange(
    val earliest: Offset,
    val latest: Offset
) {
    init {
        require(earliest.value <= latest.value) { "시작 오프셋은 마지막 오프셋보다 클 수 없습니다" }
    }

    fun getMessageCount(): Long = latest.value - earliest.value

    fun contains(offset: Offset): Boolean = offset.value >= earliest.value && offset.value <= latest.value

    fun isEmpty(): Boolean = earliest.value == latest.value

    fun isNotEmpty(): Boolean = !isEmpty()
}