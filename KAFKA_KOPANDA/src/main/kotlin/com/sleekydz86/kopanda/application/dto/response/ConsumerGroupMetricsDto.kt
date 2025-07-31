package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class ConsumerGroupMetricsDto(
    val groupId: String,
    val state: String,
    val memberCount: Int,
    val topicCount: Int,
    val totalLag: Long,
    val averageLag: Double,
    val maxLag: Long,
    val minLag: Long,
    val lastCommitTime: LocalDateTime?,
    val partitions: List<PartitionLagDto>,
    val members: List<ConsumerMemberMetricsDto>
)