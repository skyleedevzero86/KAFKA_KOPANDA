package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class ConnectionIssueDto(
    val type: String,
    val severity: String,
    val description: String,
    val timestamp: LocalDateTime
)