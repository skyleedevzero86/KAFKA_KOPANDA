package com.sleekydz86.kopanda.application.dto.response

data class PerformanceMetricsDto(
    val messagesPerSecond: Double,
    val bytesInPerSec: Double,
    val bytesOutPerSec: Double,
    val requestsPerSec: Double,
    val averageRequestLatency: Long,
    val maxRequestLatency: Long,
    val activeConnections: Int,
    val totalConnections: Int
)