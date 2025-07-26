package com.sleekydz86.kopanda.application.dto.response

import java.time.Duration
import java.time.LocalDateTime

data class ConnectionMetricsDto(
    val connectionId: String,
    val uptime: Duration,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTime: Long,
    val maxResponseTime: Long,
    val minResponseTime: Long,
    val lastActivity: LocalDateTime
)