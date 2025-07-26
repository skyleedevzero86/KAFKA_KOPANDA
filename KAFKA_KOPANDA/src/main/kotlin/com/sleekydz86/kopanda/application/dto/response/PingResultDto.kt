package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class PingResultDto(
    val connectionId: String,
    val isAlive: Boolean,
    val responseTime: Long,
    val timestamp: LocalDateTime,
    val errorMessage: String? = null
)