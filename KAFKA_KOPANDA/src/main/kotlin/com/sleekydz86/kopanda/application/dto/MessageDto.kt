package com.sleekydz86.kopanda.application.dto

data class MessageDto(
    val offset: Long,
    val key: String?,
    val value: String,
    val timestamp: Long,
    val partition: Int,
    val consumed: Boolean,
    val headers: Map<String, String>,
    val formattedTime: String,
    val formattedOffset: String
)