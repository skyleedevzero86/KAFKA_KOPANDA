package com.sleekydz86.kopanda.application.dto.common

data class PartitionDto(
    val partitionNumber: Int,
    val leader: Int,
    val replicas: List<Int>,
    val inSyncReplicas: List<Int>,
    val earliestOffset: Long,
    val latestOffset: Long,
    val messageCount: Long,
    val isHealthy: Boolean,
    val isUnderReplicated: Boolean
)