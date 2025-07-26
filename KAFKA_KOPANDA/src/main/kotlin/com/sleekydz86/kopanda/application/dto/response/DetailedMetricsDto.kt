package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class DetailedMetricsDto(
    val connectionId: String,
    val brokerMetrics: BrokerMetricsDto,
    val topicMetrics: TopicMetricsDto,
    val partitionMetrics: PartitionMetricsDto,
    val performanceMetrics: PerformanceMetricsDto,
    val timestamp: LocalDateTime
)