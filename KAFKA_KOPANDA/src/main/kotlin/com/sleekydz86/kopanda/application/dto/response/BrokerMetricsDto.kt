package com.sleekydz86.kopanda.application.dto.response

data class BrokerMetricsDto(
    val totalBrokers: Int,
    val activeBrokers: Int,
    val offlineBrokers: Int,
    val averageResponseTime: Long,
    val totalDiskUsage: Long,
    val availableDiskSpace: Long
)