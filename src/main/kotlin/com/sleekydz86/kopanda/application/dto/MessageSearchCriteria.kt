package com.sleekydz86.kopanda.application.dto

data class MessageSearchCriteria(
    val topic: String,
    val partition: Int? = null,
    val key: String? = null,
    val value: String? = null,
    val startOffset: Long? = null,
    val endOffset: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val limit: Int = 100
)