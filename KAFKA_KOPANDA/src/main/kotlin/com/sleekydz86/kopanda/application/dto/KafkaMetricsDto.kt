package com.sleekydz86.kopanda.application.dto

import java.time.LocalDateTime

data class KafkaMetricsDto(
    val brokerCount: Int,
    val topicCount: Int,
    val totalPartitions: Int,
    val messagesPerSecond: Double,
    val bytesInPerSec: Double,
    val bytesOutPerSec: Double,
    val activeConnections: Int,
    val timestamp: LocalDateTime
)