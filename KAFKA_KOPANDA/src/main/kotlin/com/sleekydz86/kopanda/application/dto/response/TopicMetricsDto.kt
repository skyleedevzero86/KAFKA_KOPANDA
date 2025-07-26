package com.sleekydz86.kopanda.application.dto.response

data class TopicMetricsDto(
    val totalTopics: Int,
    val internalTopics: Int,
    val userTopics: Int,
    val totalPartitions: Int,
    val underReplicatedPartitions: Int,
    val offlinePartitions: Int,
    val totalMessages: Long,
    val messagesPerSecond: Double
)