package com.sleekydz86.kopanda.application.dto.request

data class SendMessageRequest(
    val key: String?,
    val value: String,
    val partition: Int?,
    val headers: Map<String, String> = emptyMap()
)