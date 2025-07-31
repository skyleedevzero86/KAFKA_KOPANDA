package com.sleekydz86.kopanda.application.dto.request

data class PartitionDetailDto(
    val topicName: String,
    val partitionNumber: Int,
    val leader: Int,
    val replicas: List<Int>,
    val inSyncReplicas: List<Int>,
    val earliestOffset: Long,
    val latestOffset: Long,
    val messageCount: Long,
    val isHealthy: Boolean,
    val isUnderReplicated: Boolean,
    val lastUpdated: java.time.LocalDateTime
)