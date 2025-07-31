package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class ConnectionHealthDto(
    val connectionId: String,
    val isHealthy: Boolean,
    val healthScore: Int,
    val lastCheckTime: LocalDateTime,
    val responseTime: Long,
    val errorCount: Int,
    val successRate: Double,
    val issues: List<ConnectionIssueDto>
)