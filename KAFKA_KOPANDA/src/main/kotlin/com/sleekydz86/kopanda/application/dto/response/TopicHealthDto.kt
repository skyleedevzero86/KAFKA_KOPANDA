package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class TopicHealthDto(
    val topicName: String,
    val isHealthy: Boolean,
    val healthScore: Int,
    val underReplicatedPartitions: Int,
    val offlinePartitions: Int,
    val totalPartitions: Int,
    val replicationFactor: Int,
    val averageReplicationFactor: Double,
    val lastUpdated: LocalDateTime,
    val issues: List<TopicIssueDto>
)