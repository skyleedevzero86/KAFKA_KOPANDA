package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class ConsumerMemberMetricsDto(
    val memberId: String,
    val clientId: String,
    val clientHost: String,
    val assignedPartitions: Int,
    val totalLag: Long,
    val averageLag: Double,
    val lastHeartbeat: LocalDateTime
)