package com.sleekydz86.kopanda.application.dto.response

data class PartitionMetricsDto(
    val totalPartitions: Int,
    val healthyPartitions: Int,
    val underReplicatedPartitions: Int,
    val offlinePartitions: Int,
    val averageReplicationFactor: Double,
    val totalMessages: Long,
    val averageMessagesPerPartition: Double
)