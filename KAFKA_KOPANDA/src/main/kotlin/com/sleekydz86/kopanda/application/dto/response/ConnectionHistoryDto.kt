package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class ConnectionHistoryDto(
    val connectionId: String,
    val eventType: String,
    val description: String,
    val timestamp: LocalDateTime,
    val details: Map<String, Any> = emptyMap()
)